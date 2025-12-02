package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.*;
import com.ssafy.linkcare.health.entity.Step;
import com.ssafy.linkcare.health.repository.StepRepository;
import com.ssafy.linkcare.health.util.TimeStampUtil;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StepService {

    private final UserRepository userRepository;
    private final StepRepository stepRepository;
    private final TimeStampUtil timeStampUtil;

    @Transactional
    public void saveAllStepData(List<StepDto> stepList, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));


        List<Step> list = stepList.stream()
                .map(dto -> Step.builder()
                        .deviceId(dto.getDeviceId())
                        .deviceType(dto.getDeviceType())
                        .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .count(dto.getCount())
                        .goal(dto.getGoal())
                        .user(user)
                        .build()
                ).toList();

        stepRepository.saveAll(list);
    }

    @Transactional
    public void saveDailyStepData(StepDto dto, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if(dto == null) {

            return;
        }

        // 걸음 수 업데이트 (기존 값에 덮어쓰기)
        Long startTimeTs = dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond();
        Optional<Step> existingData = stepRepository.findByUserAndStartTime(user, startTimeTs);

        if(existingData.isPresent()) {
            Step step = existingData.get();
            step.updateStepAndGoal(dto.getCount(), dto.getGoal());
        } else {
            stepRepository.save(Step.builder()
                    .deviceId(dto.getDeviceId())
                    .deviceType(dto.getDeviceType())
                    .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                    .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                    .count(dto.getCount())
                    .goal(dto.getGoal())
                    .user(user)
                    .build()
            );
        }

    }

    // 3주치 데이터 조회
    public List<StepResponse> getStepsThreeWeeks(int userSeq, Long startTime, Long endTime) {
        User user = validateUser(userSeq);

        // 기본값 3주
        Long startTimeTs = timeStampUtil.getThreeWeeksAgoTimestamp();
        Long endTimeTs = timeStampUtil.getCurrentTimestamp();

        log.info("기간별 조회 - User: {}, Start: {}, End: {}",
                userSeq, startTimeTs, endTimeTs);

        List<Step> steps = stepRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user,startTimeTs, endTimeTs);

        return steps.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 당일 데이터 조회
    public StepResponse getTodayStep(int userSeq) {
        User user = validateUser(userSeq);

        Long startTimeTs = timeStampUtil.getTodayStartTimestamp();
        Long endTimeTs = timeStampUtil.getCurrentTimestamp();

        Step step = stepRepository
                .findByUserAndStartTime(user, startTimeTs)
                .orElse(null);

        return convertToResponse(step);
    }


    // 특정 날짜 걸음수 데이터 조회
    public int getStepsByDate(int userSeq, LocalDate date) {
        User user = validateUser(userSeq);

        Step step = stepRepository
                .findByUserAndStartTime(user, timeStampUtil.getDateStartTimestamp(date))
                .orElse(null);

        if(step == null) {
            return 0;
        } else {
            return step.getCount();
        }
    }

    // 당일 데이터 총 걸음 수 조회
    public int getTodayStepCount(int userSeq) {
        User user = validateUser(userSeq);

        Long startTimeTs = timeStampUtil.getTodayStartTimestamp();
        Long endTimeTs = timeStampUtil.getCurrentTimestamp();

        Step step = stepRepository
                .findByUserAndStartTime(user, startTimeTs)
                .orElse(null);

        if(step == null) {
            return 0;
        } else {
            return step.getCount();
        }
    }

    public List<StepResponse> getStepsByPeriod(int userSeq, Long startTime, Long endTime) {
        User user = validateUser(userSeq);

        // 기본값 3주
        Long startTimeTs = timeStampUtil.getThreeWeeksAgoTimestamp();
        Long endTimeTs = timeStampUtil.getCurrentTimestamp();

        log.info("기간별 조회 - User: {}, Start: {}, End: {}",
                userSeq, startTimeTs, endTimeTs);

        List<Step> steps = stepRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user,startTimeTs, endTimeTs);

        return steps.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    public List<StepResponse> getStepsByDate(int userSeq, LocalDate startDate, LocalDate endDate) {
        User user = validateUser(userSeq);

        Long startTimeTs = timeStampUtil.getDateStartTimestamp(startDate);
        Long endTimeTs = timeStampUtil.getDateEndTimestamp(endDate);

        log.info("(LocalDate) 기간별 조회 - User: {}, Start: {}, End: {}",
                userSeq, startTimeTs, endTimeTs);

        List<Step> steps = stepRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user,startTimeTs, endTimeTs);

        return steps.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 일주일 데이터 조회
    public List<StepResponse> getThisWeekSteps(int userSeq) {
        User user = validateUser(userSeq);

        Long weekStart = timeStampUtil.getThisWeekStartTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();

        log.info("이번 주 데이터 조회 - User: {}", userSeq);

        return getStepsByPeriod(userSeq, weekStart, now);
    }

    // 기간별 총 걸음수
    public StepStatisticsResponse getStepStatistics(int userSeq, Long startTime, Long endTime) {
        User user = validateUser(userSeq);

        Long effectiveStartTime = startTime != null ? startTime : timeStampUtil.getThreeWeeksAgoTimestamp();
        Long effectiveEndTime = endTime != null ? endTime : timeStampUtil.getCurrentTimestamp();

        log.info("물 섭취 통계 조회 - User: {}, Start: {}, End: {}", userSeq, effectiveStartTime, effectiveEndTime);

        Long totalSteps = stepRepository
                .sumTotalCountByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0L);

        Float avgSteps = stepRepository
                .avgCountByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0.0f);

        return StepStatisticsResponse.builder()
                .userSeq(Math.toIntExact(user.getUserPk()))
                .startTime(timeStampUtil.toLocalDateTime(effectiveStartTime))
                .endTime(timeStampUtil.toLocalDateTime(effectiveEndTime))
                .totalSteps(totalSteps)
                .averageSteps(avgSteps)
                .build();
    }

    // (localDate)기간별 총 걸음수
    public StepStatisticsResponse getStepStatisticsByDate(int userSeq, LocalDate startDate, LocalDate endDate) {
        User user = validateUser(userSeq);

        Long startDateTs = timeStampUtil.getDateStartTimestamp(startDate);
        Long endDateTs = timeStampUtil.getDateEndTimestamp(endDate);

        log.info("기간별 물 섭취 통계 조회 - User: {}, Start: {}, End: {}", userSeq, startDateTs, endDateTs);

        Long totalSteps = stepRepository
                .sumTotalCountByUserAndPeriod(user, startDateTs, endDateTs)
                .orElse(0L);

        Float avgSteps = stepRepository
                .avgCountByUserAndPeriod(user, startDateTs, endDateTs)
                .orElse(0.0f);

        return StepStatisticsResponse.builder()
                .userSeq(Math.toIntExact(user.getUserPk()))
                .startTime(timeStampUtil.toLocalDateTime(startDateTs))
                .endTime(timeStampUtil.toLocalDateTime(endDateTs))
                .totalSteps(totalSteps)
                .averageSteps(avgSteps)
                .build();
    }


    // 이번 주 총 걸음 수 조회
    public StepStatisticsResponse getThisWeekStepStatistics(int userSeq) {
        Long weekStart = timeStampUtil.getThisWeekStartTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();

        return getStepStatistics(userSeq, weekStart, now);
    }

//    public StepStatisticsResponse getWeekStepStatisticsByDate(int userSeq, Long startTime) {
//        Long weekStart = timeStampUtil.getWeekStartTimestamp(startTime);
//        Long weekEnd = timeStampUtil.getWeekEndTimestamp(weekStart);
//
//        return getStepStatistics(userSeq, weekStart, weekEnd);
//    }

    // 해당 날짜가 포함된 주 총 걸음 수 조회
    public StepStatisticsResponse getWeekStepStatisticsByContainDate(int userSeq, LocalDate startDate) {
        Long weekStart = timeStampUtil.getWeekStartTimestamp(startDate);
        Long weekEnd = timeStampUtil.getWeekEndTimestamp(weekStart);

        return getStepStatistics(userSeq, weekStart, weekEnd);
    }

    public StepStatisticsResponse getWeekStepStatisticsByDate(int userSeq, LocalDate startDate, LocalDate endDate) {
        Long startDateTs = timeStampUtil.getWeekStartTimestamp(startDate);
        Long endDateTs = timeStampUtil.getWeekEndTimestamp(endDate);

        return getStepStatistics(userSeq, startDateTs, endDateTs);
    }

    private StepResponse convertToResponse(Step step) {
        return StepResponse.builder()
//                .deviceType(step.getDeviceType())
//                .deviceId(step.getDeviceId())
                .count(step.getCount())
                .startTime(timeStampUtil.toLocalDateTime(step.getStartTime()))
                .endTime(timeStampUtil.toLocalDateTime(step.getStartTime()))
                .goal(step.getGoal())
//                .userSeq(Math.toIntExact(step.getUser().getUserPk()))
                .build();
    }

    /**
     * 표준 편차 계산
     * @param steps
     * @return double
     */
    public Float calculateStdDev(List<Long> steps) {
        if (steps == null || steps.isEmpty()) return 0f;

        double mean = steps.stream()
                .mapToDouble(Long::doubleValue)
                .average()
                .orElse(0.0);

        double variance = steps.stream()
                .mapToDouble(v -> {
                    double diff = v - mean;
                    return diff * diff;
                })
                .average()
                .orElse(0.0); // 모집단 분산

        return (float) Math.sqrt(variance);
    }

    private User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
