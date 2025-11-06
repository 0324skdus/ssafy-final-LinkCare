package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.WaterIntakeDto;
import com.ssafy.linkcare.health.dto.WaterIntakeGroupedDto;
import com.ssafy.linkcare.health.entity.WaterIntake;
import com.ssafy.linkcare.health.repository.WaterIntakeRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Slf4j
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
                            .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
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

    @Transactional
    public void saveDailyWaterIntakeData(WaterIntakeDto waterIntakeDto, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 목표 x 음수량 x
        if(waterIntakeDto.getGoal() == 0 &&(waterIntakeDto.getWaterIntakes() == null || waterIntakeDto.getWaterIntakes().isEmpty())) {
            return;
        }

        waterIntakeDto.getWaterIntakes().forEach(dto -> {
            // 이미 데이터가 존재하면 교체!
            Optional<WaterIntake> existingData = waterIntakeRepository.findByUserAndUid(user, dto.getUid());

            if(existingData.isPresent()) {
                // 수정만 가능
                WaterIntake waterIntake = existingData.get();
                waterIntake.updateAmountAndGoal(dto.getAmount(), waterIntakeDto.getGoal());
            } else {
                waterIntakeRepository.save(WaterIntake.builder()
                        .deviceId(dto.getDeviceId())
                        .deviceType(dto.getDeviceType())
                        .uid(dto.getUid())
                        .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .dataSource(dto.getDataSource())
                        .zoneOffset(dto.getZoneOffset())
                        .amount(dto.getAmount())
                        .goal(waterIntakeDto.getGoal())
                        .user(user)
                        .build()
                );
            }
        });



    };
}

