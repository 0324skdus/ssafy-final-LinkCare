package com.ssafy.linkcare.ai.service;

import com.ssafy.linkcare.ai.client.AiClient;
import com.ssafy.linkcare.ai.dto.*;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.group.dto.UpdateWeeklyGroupGoalRequest;
import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupGoalCriteria;
import com.ssafy.linkcare.group.entity.WeeklyGroupGoals;
import com.ssafy.linkcare.group.entity.WeeklyGroupStats;
import com.ssafy.linkcare.group.enums.MetricType;
import com.ssafy.linkcare.group.repository.GroupGoalCriteriaRepository;
import com.ssafy.linkcare.group.repository.GroupRepository;
import com.ssafy.linkcare.group.repository.WeeklyGroupGoalsRepository;
import com.ssafy.linkcare.group.repository.WeeklyGroupStatsRepository;
import com.ssafy.linkcare.health.service.StepService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

/**
 * AI 기반 그룹 목표 생성 서비스
 * - 최근 3주 데이터 기반으로 성장률 예측
 * - 4가지 메트릭(Steps, Kcal, Duration, Distance)에 대한 목표 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoalGenerationService {

    private final AiClient aiClient;
    private final GroupRepository groupRepository;
    private final WeeklyGroupStatsRepository weeklyGroupStatsRepository;
    private final WeeklyGroupGoalsRepository weeklyGroupGoalsRepository;
    private final GroupGoalCriteriaRepository groupGoalCriteriaRepository;
    private final StepService stepService;

    /**
     * 그룹 목표 생성
     * @param groupSeq 그룹 시퀀스
     * @param requestDate 요청 날짜 (어떤 요일이든 가능)
     * @return 생성된 목표
     */
    @Transactional
    public WeeklyGroupGoals generateNextWeekGoal(Long groupSeq, LocalDate requestDate) {

        // 1. 그룹 조회
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        log.info("그룹 목표 생성 시작: groupSeq={}, groupName={}, requestDate={}",
                groupSeq, group.getGroupName(), requestDate);

        // 2. 요청 날짜가 속한 주의 월요일 계산
        LocalDate weekStart = requestDate.with(DayOfWeek.MONDAY);
        log.info("이번 주 시작일(월요일): {}", weekStart);

        // 3. 중복 체크 - 이미 해당 주차 목표가 있으면 반환 (멱등성)
        return generateNewGoal(group, weekStart);

//        // 3. 중복 체크 - 이미 해당 주차 목표가 있으면 반환 (멱등성)
//        return weeklyGroupGoalsRepository.findByGroup_GroupSeqAndWeekStart(groupSeq, weekStart)
//                .orElseGet(() -> generateNewGoal(group, weekStart));
    }

    /**
     * 새로운 주간 목표 생성 (AI 예측 기반)
     */
    private WeeklyGroupGoals generateNewGoal(Group group, LocalDate weekStart) {
        Long groupSeq = group.getGroupSeq();
        log.info("새로운 목표 생성 시작: groupSeq={}, weekStart={}", groupSeq, weekStart);

        // 1. 저번 주부터 과거로 최대 3주 데이터 조회
        List<WeeklyGroupStats> allWeeks = weeklyGroupStatsRepository
                .findTop3ByGroup_GroupSeqOrderByWeekStartDesc(groupSeq);

        // 저번 주 이전 데이터만 필터링 (이번 주 데이터는 제외)
        List<WeeklyGroupStats> pastWeeks = allWeeks.stream()
                .filter(stats -> stats.getWeekStart().isBefore(weekStart))
                .toList();

        if (pastWeeks.isEmpty()) {
            log.error("그룹 {}의 과거 통계 데이터가 존재하지 않습니다.", group.getGroupName());
            throw new CustomException(ErrorCode.INSUFFICIENT_DATA);
        }

        log.info("사용 가능한 과거 주간 데이터: {}주 (최대 3주)", pastWeeks.size());

        // 2. 최신 주차 정보 (가장 최근 과거 데이터)
        WeeklyGroupStats latest = pastWeeks.get(0);

        // 3. 평균 계산 (사용 가능한 주차만큼)
        double stepsMean3w = pastWeeks.stream()
                .mapToLong(WeeklyGroupStats::getGroupStepsTotal)
                .average()
                .orElse(0.0);

        double kcalMean3w = pastWeeks.stream()
                .mapToDouble(WeeklyGroupStats::getGroupKcalTotal)
                .average()
                .orElse(0.0);

        double durationMean3w = pastWeeks.stream()
                .mapToInt(WeeklyGroupStats::getGroupDurationTotal)
                .average()
                .orElse(0.0);

        double distanceMean3w = pastWeeks.stream()
                .mapToDouble(WeeklyGroupStats::getGroupDistanceTotal)
                .average()
                .orElse(0.0);

        // 6. 표준편차 계산 (사용 가능한 주차만큼)
        double stepsStd3w = calculateStandardDeviation(
                pastWeeks.stream()
                        .map(s -> (double) s.getGroupStepsTotal())
                        .toList()
        );

        double kcalStd3w = calculateStandardDeviation(
                pastWeeks.stream()
                        .map(s -> (double) s.getGroupKcalTotal())
                        .toList()
        );

        double durationStd3w = calculateStandardDeviation(
                pastWeeks.stream()
                        .map(s -> (double) s.getGroupDurationTotal())
                        .toList()
        );

        double distanceStd3w = calculateStandardDeviation(
                pastWeeks.stream()
                        .map(s -> (double) s.getGroupDistanceTotal())
                        .toList()
        );

        log.info("통계 계산 완료 ({}주 데이터) - stepsMean={}, kcalMean={}, durationMean={}, distanceMean={}",
                pastWeeks.size(), stepsMean3w, kcalMean3w, durationMean3w, distanceMean3w);

        // 7. AI 요청 DTO 구성 및 호출
        StepsPredictRequest stepsReq = StepsPredictRequest.builder()
                .member_count(latest.getMemberCount())
                .avg_age(latest.getAvgAge())
                .avg_bmi(latest.getAvgBmi())
                .group_steps_mean_3w(stepsMean3w)
                .group_steps_var_3w(stepsStd3w)  // 표준편차 값을 var 필드명으로 전송
                .group_duration_mean_3w(durationMean3w)
                .member_steps_var(latest.getMemberStepsVar())
                .build();

        KcalPredictRequest kcalReq = KcalPredictRequest.builder()
                .member_count(latest.getMemberCount())
                .avg_age(latest.getAvgAge())
                .avg_bmi(latest.getAvgBmi())
                .group_kcal_mean_3w(kcalMean3w)
                .group_kcal_var_3w(kcalStd3w)  // 표준편차 값을 var 필드명으로 전송
                .group_duration_mean_3w(durationMean3w)
                .member_steps_var(latest.getMemberStepsVar())
                .build();

        DurationPredictRequest durationReq = DurationPredictRequest.builder()
                .member_count(latest.getMemberCount())
                .avg_age(latest.getAvgAge())
                .avg_bmi(latest.getAvgBmi())
                .group_duration_mean_3w(durationMean3w)
                .group_duration_var_3w(durationStd3w)  // 표준편차 값을 var 필드명으로 전송
                .group_steps_mean_3w(stepsMean3w)
                .member_steps_var(latest.getMemberStepsVar())
                .build();

        DistancePredictRequest distanceReq = DistancePredictRequest.builder()
                .member_count(latest.getMemberCount())
                .avg_age(latest.getAvgAge())
                .avg_bmi(latest.getAvgBmi())
                .group_distance_mean_3w(distanceMean3w)
                .group_distance_var_3w(distanceStd3w)  // 표준편차 값을 var 필드명으로 전송
                .group_duration_mean_3w(durationMean3w)
                .member_steps_var(latest.getMemberStepsVar())
                .build();

        // 8. FastAPI 4개 호출
        AiPredictResponse stepsRes = aiClient.predictSteps(stepsReq);
        AiPredictResponse kcalRes = aiClient.predictKcal(kcalReq);
        AiPredictResponse durationRes = aiClient.predictDuration(durationReq);
        AiPredictResponse distanceRes = aiClient.predictDistance(distanceReq);

        log.info("AI 예측 완료 - stepsGrowth={}, kcalGrowth={}, durationGrowth={}, distanceGrowth={}",
                stepsRes.getPredicted_growth_rate(), kcalRes.getPredicted_growth_rate(),
                durationRes.getPredicted_growth_rate(), distanceRes.getPredicted_growth_rate());

        // 9. 주간 목표 계산 = 평균값 × 성장률
        long weeklyGoalSteps = Math.round(stepsMean3w * stepsRes.getPredicted_growth_rate());
        float weeklyGoalKcal = (float) (kcalMean3w * kcalRes.getPredicted_growth_rate());
        int weeklyGoalDuration = (int) Math.round(durationMean3w * durationRes.getPredicted_growth_rate());
        float weeklyGoalDistance = (float) (distanceMean3w * distanceRes.getPredicted_growth_rate());

        log.info("주간 목표 계산 완료 - steps={}, kcal={}, duration={}분, distance={}km",
                weeklyGoalSteps, weeklyGoalKcal, weeklyGoalDuration, weeklyGoalDistance);

        // 10. 기존 목표 확인 및 저장/업데이트
        WeeklyGroupGoals goal = weeklyGroupGoalsRepository
                .findByGroup_GroupSeqAndWeekStart(groupSeq, weekStart)
                .orElse(null);

        if (goal != null) {
            // 기존 목표가 있으면 업데이트
            log.info("기존 목표 발견 - 업데이트 수행: weeklyGroupGoalsSeq={}", goal.getWeeklyGroupGoalsSeq());
            goal.updateFromAiPrediction(
                    weeklyGoalSteps, weeklyGoalKcal, weeklyGoalDuration, weeklyGoalDistance,
                    stepsRes.getPredicted_growth_rate(), kcalRes.getPredicted_growth_rate(),
                    durationRes.getPredicted_growth_rate(), distanceRes.getPredicted_growth_rate()
            );
        } else {
            // 새로운 목표 생성
            log.info("기존 목표 없음 - 새로운 목표 생성");
            goal = WeeklyGroupGoals.builder()
                    .group(group)
                    .weekStart(weekStart)
                    .goalSteps(weeklyGoalSteps)
                    .goalKcal(weeklyGoalKcal)
                    .goalDuration(weeklyGoalDuration)
                    .goalDistance(weeklyGoalDistance)
                    .predictedGrowthRateSteps(stepsRes.getPredicted_growth_rate())
                    .predictedGrowthRateKcal(kcalRes.getPredicted_growth_rate())
                    .predictedGrowthRateDuration(durationRes.getPredicted_growth_rate())
                    .predictedGrowthRateDistance(distanceRes.getPredicted_growth_rate())
                    .selectedMetricType(null)  // 초기 생성 시 선택 안 함
                    .createdAt(System.currentTimeMillis())
                    .build();
        }

        WeeklyGroupGoals saved = weeklyGroupGoalsRepository.save(goal);

        log.info("그룹 목표 저장 완료: groupSeq={}, weekStart={}, goalSteps={}, goalKcal={}, goalDuration={}, goalDistance={}",
                groupSeq, weekStart, weeklyGoalSteps, weeklyGoalKcal, weeklyGoalDuration, weeklyGoalDistance);

        return saved;
    }

    /**
     * 현재 주차 목표 조회
     * @param groupSeq 그룹 시퀀스
     * @return 현재 주차 목표 (없으면 Optional.empty())
     */
    @Transactional(readOnly = true)
    public WeeklyGroupGoals getCurrentWeekGoal(Long groupSeq) {
        // 그룹 존재 여부 확인
        if (!groupRepository.existsById(groupSeq)) {
            throw new CustomException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 이번 주 월요일 계산
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        log.info("현재 주차 목표 조회: groupSeq={}, weekStart={}", groupSeq, weekStart);

        // 현재 주차 목표 조회
        return weeklyGroupGoalsRepository.findByGroup_GroupSeqAndWeekStart(groupSeq, weekStart)
                .orElseThrow(() -> new CustomException(ErrorCode.GOAL_NOT_FOUND));
    }

    /**
     * 현재 주차 목표 수정 또는 생성
     * @param groupSeq 그룹 시퀀스
     * @param request 수정 요청 (selectedMetricType, goalValue)
     * @return 수정 또는 생성된 목표
     */
    @Transactional
    public WeeklyGroupGoals updateCurrentWeekGoal(Long groupSeq, UpdateWeeklyGroupGoalRequest request) {
        // 그룹 존재 여부 확인
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 이번 주 월요일 계산
        LocalDate weekStart = LocalDate.now().with(DayOfWeek.MONDAY);

        log.info("현재 주차 목표 수정/생성 시작: groupSeq={}, weekStart={}, metricType={}, goalValue={}",
                groupSeq, weekStart, request.selectedMetricType(), request.goalValue());

        // 현재 주차 목표 조회 또는 생성
        WeeklyGroupGoals goal = weeklyGroupGoalsRepository.findByGroup_GroupSeqAndWeekStart(groupSeq, weekStart)
                .orElseGet(() -> {
                    log.info("기존 목표가 없어 새로 생성합니다: groupSeq={}, weekStart={}", groupSeq, weekStart);

                    // 기본값으로 새 목표 생성 (모든 필드를 0으로 초기화)
                    WeeklyGroupGoals newGoal = WeeklyGroupGoals.builder()
                            .group(group)
                            .weekStart(weekStart)
                            .goalSteps(0L)
                            .goalKcal(0.0f)
                            .goalDuration(0)
                            .goalDistance(0.0f)
                            .predictedGrowthRateSteps(1.0)
                            .predictedGrowthRateKcal(1.0)
                            .predictedGrowthRateDuration(1.0)
                            .predictedGrowthRateDistance(1.0)
                            .createdAt(System.currentTimeMillis())
                            .build();

                    return weeklyGroupGoalsRepository.save(newGoal);
                });

        // 최소 기준 검증 및 조정
        Long finalGoalValue = validateAndAdjustGoalValue(
                groupSeq,
                request.selectedMetricType(),
                request.goalValue()
        );

        // 목표 업데이트
        goal.updateGoal(request.selectedMetricType(), finalGoalValue);

        WeeklyGroupGoals saved = weeklyGroupGoalsRepository.save(goal);

        log.info("목표 수정/생성 완료: groupSeq={}, metricType={}, finalValue={}",
                groupSeq, request.selectedMetricType(), finalGoalValue);

        return saved;
    }

    /**
     * 최소 기준 검증 및 조정
     * - 설정된 최소값보다 작으면 최소값으로 조정
     */
    private Long validateAndAdjustGoalValue(Long groupSeq, MetricType metricType, Long goalValue) {
        // GroupGoalCriteria 조회 (헬스 그룹만 해당, 없으면 검증 스킵)
        GroupGoalCriteria criteria = groupGoalCriteriaRepository.findByGroup_GroupSeq(groupSeq)
                .orElse(null);

        if (criteria == null) {
            return goalValue;  // 기준이 없으면 그대로 사용
        }

        // 메트릭 타입별 최소값 가져오기
        Number minValue = switch (metricType) {
            case STEPS -> criteria.getMinStep();
            case KCAL -> criteria.getMinCalorie();
            case DURATION -> criteria.getMinDuration();
            case DISTANCE -> criteria.getMinDistance();
        };

        // 최소값이 설정되지 않았으면 그대로 사용
        if (minValue == null) {
            return goalValue;
        }

        // 최소값보다 작으면 최소값으로 조정
        long minValueLong = minValue.longValue();
        if (goalValue < minValueLong) {
            log.info("목표값이 최소값보다 작아 조정: {} -> {}", goalValue, minValueLong);
            return minValueLong;
        }

        return goalValue;
    }

    /**
     * 표준편차 계산
     */
    private double calculateStandardDeviation(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        double mean = values.stream()
                .mapToDouble(v -> v)
                .average()
                .orElse(0.0);

        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }
}
