package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.ActivitySummaryDto;
import com.ssafy.linkcare.health.dto.ActivitySummaryResponse;
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

    // 3주치 데이터 조회
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

        return activitySummaryRepository
                .findByUserAndStartTime(user, startTimeTs)
                .map(this::convertToResponse)
                .orElseGet(() -> createEmptyActivitySummaryResponse(startTimeTs));
    }

    // 일주일 데이터 조회
    public List<ActivitySummaryResponse> getThisWeekActivitySummaries(int userSeq) {
        User user = validateUser(userSeq);

        Long weekStart = timeStampUtil.getThisWeekStartTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();

        log.info("이번 주 데이터 조회 - User: {}", userSeq);

        return getActivitySummariesByPeriod(userSeq, weekStart, now);
    }



    private ActivitySummaryResponse convertToResponse(ActivitySummary activitySummary) {
        return ActivitySummaryResponse.builder()
                .totalDistance(activitySummary.getTotalDistance())
//                .activitySummaryId(activitySummary.getActivitySummaryId())
                .deviceId(activitySummary.getDeviceId())
                .deviceType(activitySummary.getDeviceType())
                .totalCaloriesBurned(activitySummary.getTotalCaloriesBurned())
                .startTime(timeStampUtil.toLocalDateTime(activitySummary.getStartTime()))
                .build();
    }

    // Helper 메서드
    private ActivitySummaryResponse createEmptyActivitySummaryResponse(Long startTime) {
        return ActivitySummaryResponse.builder()
                .deviceId(null)
                .deviceType(null)
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
