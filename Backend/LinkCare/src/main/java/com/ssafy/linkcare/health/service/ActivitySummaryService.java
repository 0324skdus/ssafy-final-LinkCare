package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.ActivitySummaryDto;
import com.ssafy.linkcare.health.dto.ActivitySummaryResponse;
import com.ssafy.linkcare.health.dto.ActivitySummaryStaticsResponse;
import com.ssafy.linkcare.health.entity.ActivitySummary;
import com.ssafy.linkcare.health.repository.ActivitySummaryRepository;
import com.ssafy.linkcare.health.util.TimeStampUtil;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivitySummaryService {

    private final UserRepository userRepository;
    private final ActivitySummaryRepository activitySummaryRepository;

    private final TimeStampUtil timeStampUtil;

    // 전체 데이터 저장
    @Transactional
    public void saveAllActivitySummaryData(List<ActivitySummaryDto> activityList, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));



        List<ActivitySummary> list = activityList.stream()
                .map(dto -> ActivitySummary.builder()
                        .deviceId(dto.getDeviceId())
                        .deviceType(dto.getDeviceType())
                        .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .totalCaloriesBurned(dto.getTotalCaloriesBurned())
                        .totalDistance(dto.getTotalDistance())
                        .user(user)
                        .build())
                .toList();

        activitySummaryRepository.saveAll(list);
    }


    // 데일리 데이터 저장
    @Transactional
    public void saveDailyActivitySummaryData(ActivitySummaryDto activityDto, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if(activityDto == null || (activityDto.getTotalCaloriesBurned() == 0 && activityDto.getTotalDistance() == 0)) {
            return;
        }
        Long startTimeTs = activityDto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond();

        Optional<ActivitySummary> existingData = activitySummaryRepository.findByUserAndStartTime(user, startTimeTs);
        if(existingData.isPresent()) {
            // 업데이트만 가능
            ActivitySummary activitySummary = existingData.get();
            activitySummary.updateCaloriesAndDistance(activityDto.getTotalCaloriesBurned(), activitySummary.getTotalDistance());
        } else {
            activitySummaryRepository.save(ActivitySummary.builder()
                    .startTime(startTimeTs)
                    .totalDistance(activityDto.getTotalDistance())
                    .totalCaloriesBurned(activityDto.getTotalCaloriesBurned())
                    .user(user)
                    .build());
        }
    }

    // 기간별 데이터 조회
    public List<ActivitySummaryResponse> getActivitySummariesByPeriod(int userSeq, Long startTime, Long endTime) {
        User user = validateUser(userSeq);

        // 기본값 3주
        Long startTimeTs = timeStampUtil.getThreeWeeksAgoTimestamp();
        Long endTimeTs = timeStampUtil.getCurrentTimestamp();

        log.info("기간별 조회 - User: {}, Start: {}, End: {}",
                userSeq, startTimeTs, endTimeTs);

        List<ActivitySummary> activitySummaries = activitySummaryRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user,startTimeTs, endTimeTs);

        return activitySummaries.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 당일 데이터 조회
    public ActivitySummaryResponse getTodayActivitySummary(int userSeq) {
        User user = validateUser(userSeq);

        Long startTimeTs = timeStampUtil.getTodayStartTimestamp();
        Long endTimeTs = timeStampUtil.getCurrentTimestamp();

        ActivitySummary activitySummary = activitySummaryRepository
                        .findByUserAndStartTime(user, startTimeTs)
                        .orElse(null);

        if(activitySummary == null) {
            return ActivitySummaryResponse.builder()
                    .startTime(timeStampUtil.toLocalDateTime(startTimeTs))
                    .totalCaloriesBurned(0)
                    .totalDistance(0)
                    .build();
        }

        return convertToResponse(activitySummary);
    }

    // 일주일 데이터 조회
    public List<ActivitySummaryResponse> getThisWeekActivitySummaries(int userSeq) {
        User user = validateUser(userSeq);

        Long weekStart = timeStampUtil.getThisWeekStartTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();

        log.info("이번 주 데이터 조회 - User: {}", userSeq);

        return getActivitySummariesByPeriod(userSeq, weekStart, now);
    }

    // 오늘 기준 바로 전 주 데이터 조회
    public List<ActivitySummaryResponse> getLastWeekActivitySummaries(int userSeq, LocalDate today) {
        User user = validateUser(userSeq);

        Long startDateTs = timeStampUtil.toTimestamp(timeStampUtil.getLastWeekMonday());
        Long endDateTs = timeStampUtil.toTimestamp(timeStampUtil.getLastWeekSunday());

        List<ActivitySummary> summaries = activitySummaryRepository.findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, startDateTs, endDateTs);

        return summaries.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 기간별 활동 요약 조회
    public List<ActivitySummaryResponse> getActivitySummariesByDate(int userSeq, LocalDate startDate, LocalDate endDate) {
        User user = validateUser(userSeq);

        Long startDateTs = timeStampUtil.toTimestamp(startDate);
        Long endDateTs = timeStampUtil.toTimestamp(endDate);

        List<ActivitySummary> summaries = activitySummaryRepository.findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, startDateTs, endDateTs);

        return summaries.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    /**
     * 기간별 활동 요약 통계 조회
     */
    public ActivitySummaryStaticsResponse getActivitySummaryStatsByDate(int userSeq, LocalDate startDate, LocalDate endDate) {
        User user = validateUser(userSeq);

        long startTimestamp = timeStampUtil.getDateStartTimestamp(startDate);
        long endTimestamp = timeStampUtil.getDateEndTimestamp(endDate);

        Double totalCalories = activitySummaryRepository.findTotalCaloriesByPeriod(user, startTimestamp, endTimestamp);
        Double totalDistance = activitySummaryRepository.findTotalDistanceByPeriod(user, startTimestamp, endTimestamp);
        Double avgCalories = activitySummaryRepository.findAvgCaloriesByPeriod(user, startTimestamp, endTimestamp);
        Double avgDistance = activitySummaryRepository.findAvgDistanceByPeriod(user, startTimestamp, endTimestamp);

        return ActivitySummaryStaticsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .totalCalories(totalCalories != null ? totalCalories : 0.0)
                .totalDistance(totalDistance != null ? totalDistance : 0.0)
                .avgCalories(avgCalories != null ? avgCalories : 0.0)
                .avgDistance(avgDistance != null ? avgDistance : 0.0)
                .build();
    }

    public ActivitySummaryStaticsResponse getActivitySummaryStatsToday(int userSeq) {
        User user = validateUser(userSeq);

        long startTimestamp = timeStampUtil.getTodayStartTimestamp();
        long endTimestamp = timeStampUtil.getDateEndTimestamp(LocalDate.now());

        Double totalCalories = activitySummaryRepository.findTotalCaloriesByPeriod(user, startTimestamp, endTimestamp);
        Double totalDistance = activitySummaryRepository.findTotalDistanceByPeriod(user, startTimestamp, endTimestamp);
        Double avgCalories = activitySummaryRepository.findAvgCaloriesByPeriod(user, startTimestamp, endTimestamp);
        Double avgDistance = activitySummaryRepository.findAvgDistanceByPeriod(user, startTimestamp, endTimestamp);

        return ActivitySummaryStaticsResponse.builder()
                .startDate(timeStampUtil.toLocalDate(startTimestamp))
                .endDate(timeStampUtil.toLocalDate(endTimestamp))
                .totalCalories(totalCalories != null ? totalCalories : 0.0)
                .totalDistance(totalDistance != null ? totalDistance : 0.0)
                .avgCalories(avgCalories != null ? avgCalories : 0.0)
                .avgDistance(avgDistance != null ? avgDistance : 0.0)
                .build();
    }

    private ActivitySummaryResponse convertToResponse(ActivitySummary activitySummary) {
        return ActivitySummaryResponse.builder()
                .totalDistance(activitySummary.getTotalDistance())
                .totalCaloriesBurned(activitySummary.getTotalCaloriesBurned())
                .startTime(timeStampUtil.toLocalDateTime(activitySummary.getStartTime()))
                .build();
    }

    // Helper 메서드
    private ActivitySummaryResponse createEmptyActivitySummaryResponse(Long startTime) {
        return ActivitySummaryResponse.builder()
                .startTime(timeStampUtil.toLocalDateTime(startTime))
                .totalCaloriesBurned(0.0)
                .totalDistance(0.0)
                .build();
    }

    private User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
