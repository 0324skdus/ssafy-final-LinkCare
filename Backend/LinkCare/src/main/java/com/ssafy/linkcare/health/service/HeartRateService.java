package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.HeartRateDto;
import com.ssafy.linkcare.health.entity.HeartRate;
import com.ssafy.linkcare.health.repository.HeartRateRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HeartRateService {

    private final UserRepository userRepository;
    private final HeartRateRepository heartRateRepository;

    @Transactional
    public void saveAllHeartRateData(List<HeartRateDto> heartRateList, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<HeartRate> list = heartRateList.stream()
                .map(dto -> HeartRate.builder()
                        .deviceId(dto.getDeviceId())
                        .deviceType(dto.getDeviceType())
                        .uid(dto.getUid())
                        .zoneOffset(dto.getZoneOffset())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .dataSource(dto.getDataSource())
                        .heartRate(dto.getHeartRate())
                        .user(user)
                        .build()
                ).toList();

        heartRateRepository.saveAll(list);
    }

    @Transactional
    public void saveDailyHeartRateData(List<HeartRateDto> heartRateList, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<HeartRate> list = heartRateList.stream()
                .map(dto -> HeartRate.builder()
                        .deviceId(dto.getDeviceId())
                        .deviceType(dto.getDeviceType())
                        .uid(dto.getUid())
                        .zoneOffset(dto.getZoneOffset())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .dataSource(dto.getDataSource())
                        .heartRate(dto.getHeartRate())
                        .user(user)
                        .build()
                ).toList();

        heartRateRepository.saveAll(list);
    }
}
