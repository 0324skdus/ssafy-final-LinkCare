package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.ActivitySummaryDto;
import com.ssafy.linkcare.health.entity.ActivitySummary;
import com.ssafy.linkcare.health.repository.ActivitySummaryRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
                        .startTime(dto.getStartTime())
                        .totalCaloriesBurned(dto.getTotalCaloriesBurned())
                        .totalDistance(dto.getTotalDistance())
                        .user(user)
                        .build())
                .toList();

        activitySummaryRepository.saveAll(list);
    }

}
