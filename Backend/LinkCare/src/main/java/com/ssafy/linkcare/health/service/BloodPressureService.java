package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.BloodPressureDto;
import com.ssafy.linkcare.health.entity.BloodPressure;
import com.ssafy.linkcare.health.repository.BloodPressureRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BloodPressureService {

    private final UserRepository userRepository;
    private final BloodPressureRepository bloodPressureRepository;

    @Transactional
    public void saveAllBloodPressureData(List<BloodPressureDto> bloodPressureList, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<BloodPressure> list = bloodPressureList.stream()
                .map(dto -> BloodPressure.builder()
                        .deviceId(dto.getDeviceId())
                        .deviceType(dto.getDeviceType())
                        .uid(dto.getUid())
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .dataSource(dto.getDataSource())
                        .systolic(dto.getSystolic())
                        .diastolic(dto.getDiastolic())
                        .mean(dto.getMean())
                        .pulseRate(dto.getPulseRate())
                        .user(user)
                        .build()
                ).toList();

        bloodPressureRepository.saveAll(list);
    }
}
