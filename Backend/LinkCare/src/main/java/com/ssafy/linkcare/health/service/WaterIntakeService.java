package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.WaterIntakeDto;
import com.ssafy.linkcare.health.dto.WaterIntakeGroupedDto;
import com.ssafy.linkcare.health.entity.WaterIntake;
import com.ssafy.linkcare.health.repository.WaterIntakeRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WaterIntakeService {

    private final UserRepository userRepository;
    private final WaterIntakeRepository waterIntakeRepository;

    @Transactional
    public void saveAllWaterIntakeData(List<WaterIntakeDto> waterIntakeList, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        waterIntakeList.forEach(waterIntakeDto -> {

            if(waterIntakeDto.getWaterIntakes() == null || waterIntakeDto.getWaterIntakes().isEmpty()) {
                return;
            }

            List<WaterIntake> list = waterIntakeDto.getWaterIntakes().stream()
                    .map(dto -> WaterIntake.builder()
                            .deviceId(dto.getDeviceId())
                            .deviceType(dto.getDeviceType())
                            .uid(dto.getUid())
                            .startTime(dto.getStartTime())
                            .dataSource(dto.getDataSource())
                            .zoneOffset(dto.getZoneOffset())
                            .amount(dto.getAmount())
                            .goal(waterIntakeDto.getGoal())
                            .user(user)
                            .build()
                    ).toList();

            waterIntakeRepository.saveAll(list);
        });

    }
}
