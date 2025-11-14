package com.ssafy.linkcare.group.service;

import com.ssafy.linkcare.group.entity.GroupGoalRecord;
import com.ssafy.linkcare.group.entity.WeeklyGroupGoals;
import com.ssafy.linkcare.group.entity.WeeklyGroupStats;
import com.ssafy.linkcare.group.repository.GroupGoalRecordRepository;
import com.ssafy.linkcare.group.repository.WeeklyGroupGoalsRepository;
import com.ssafy.linkcare.group.repository.WeeklyGroupStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 그룹 목표 달성 기록 생성 서비스
 * - 비동기로 각 그룹의 주간 목표 달성 기록 생성
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class GoalRecordService {

    private final GroupGoalRecordRepository recordRepository;
    private final WeeklyGroupGoalsRepository goalsRepository;
    private final WeeklyGroupStatsRepository statsRepository;

    /**
     * 그룹별 주간 목표 달성 기록 생성 (비동기)
     * - 큐에 쌓인 작업을 순차적으로 처리
     *
     * @param groupSeq 그룹 시퀀스
     * @param weekStart 주차 시작일 (월요일)
     */
    @Async("goalRecordExecutor")
    @Transactional
    public void processGroupAsync(Long groupSeq, LocalDate weekStart) {
        try {
            // 중복 체크
            Optional<GroupGoalRecord> existingRecord = recordRepository
                    .findByGroupSeqAndWeekStart(groupSeq, weekStart);

            if (existingRecord.isPresent()) {
                log.info("그룹 {}의 {} 주차 기록이 이미 존재 - 스킵", groupSeq, weekStart);
                return;
            }

            // 목표 조회
            Optional<WeeklyGroupGoals> goalOpt = goalsRepository
                    .findByGroup_GroupSeqAndWeekStart(groupSeq, weekStart);

            if (goalOpt.isEmpty()) {
                log.info("그룹 {}의 {} 주차 목표 없음 - 스킵", groupSeq, weekStart);
                return;
            }

            WeeklyGroupGoals goal = goalOpt.get();

            // 실제 달성 데이터 조회
            Optional<WeeklyGroupStats> statsOpt = statsRepository
                    .findByGroup_GroupSeqAndWeekStart(groupSeq, weekStart);

            if (statsOpt.isEmpty()) {
                log.warn("그룹 {}의 {} 주차 통계 없음 - 스킵", groupSeq, weekStart);
                return;
            }

            WeeklyGroupStats stats = statsOpt.get();

            // 달성률 계산
            float stepsRate = calculateRate(stats.getGroupStepsTotal(), goal.getGoalSteps());
            float kcalRate = calculateRate(stats.getGroupKcalTotal(), goal.getGoalKcal());
            float durationRate = calculateRate(stats.getGroupDurationTotal(), goal.getGoalDuration());
            float distanceRate = calculateRate(stats.getGroupDistanceTotal(), goal.getGoalDistance());

            // 성공 여부 (모든 항목 100% 이상 달성)
            boolean isSucceeded = stepsRate >= 100 || kcalRate >= 100
                    || durationRate >= 100 || distanceRate >= 100;

            // 기록 생성
            GroupGoalRecord record = GroupGoalRecord.builder()
                    .groupSeq(groupSeq)
                    .weekStart(weekStart)
                    .goalSteps(goal.getGoalSteps())
                    .actualSteps(stats.getGroupStepsTotal())
                    .achievementRateSteps(stepsRate)
                    .goalKcal(goal.getGoalKcal())
                    .actualKcal(stats.getGroupKcalTotal())
                    .achievementRateKcal(kcalRate)
                    .goalDuration(goal.getGoalDuration())
                    .actualDuration(stats.getGroupDurationTotal())
                    .achievementRateDuration(durationRate)
                    .goalDistance(goal.getGoalDistance())
                    .actualDistance(stats.getGroupDistanceTotal())
                    .achievementRateDistance(distanceRate)
                    .isSucceeded(isSucceeded)
                    .build();

            recordRepository.save(record);

            log.info("그룹 {}의 {} 주차 기록 생성 완료 (성공: {})", groupSeq, weekStart, isSucceeded);

        } catch (Exception e) {
            log.error("그룹 {}의 {} 주차 기록 생성 실패: {}", groupSeq, weekStart, e.getMessage(), e);
            // 예외 발생해도 다른 그룹 처리는 계속됨
        }
    }

    /**
     * 달성률 계산
     *
     * @param actual 실제 값
     * @param goal   목표 값
     * @return 달성률 (%)
     */
    private float calculateRate(Number actual, Number goal) {
        if (goal.doubleValue() == 0) {
            return 0f;
        }
        return (float) ((actual.doubleValue() / goal.doubleValue()) * 100);
    }
}
