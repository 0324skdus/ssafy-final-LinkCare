package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.ActivitySummaryDto;
import com.ssafy.linkcare.health.entity.ActivitySummary;
import com.ssafy.linkcare.health.repository.ActivitySummaryRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ActivitySummaryService {

    private final UserRepository userRepository;
    private final ActivitySummaryRepository activitySummaryRepository;

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

}
