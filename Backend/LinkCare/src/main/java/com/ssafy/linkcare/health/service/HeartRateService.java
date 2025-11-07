package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.BloodPressureDto;
import com.ssafy.linkcare.health.dto.HeartRateDto;
import com.ssafy.linkcare.health.entity.BloodPressure;
import com.ssafy.linkcare.health.entity.HeartRate;
import com.ssafy.linkcare.health.repository.HeartRateRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
                        .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
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

        if(heartRateList == null || heartRateList.isEmpty()) {
            return;
        }

        // 삼성 헬스 uids
        Set<String> sdkUids = heartRateList.stream().map(HeartRateDto::getUid).collect(Collectors.toSet());

        // 동기화 날짜
        LocalDate syncDate = heartRateList.get(0).getStartTime().toLocalDate();

        // db에 저장되어 있는 혈압
        List<HeartRate> heartRates = heartRateRepository.findByUserAndStartDate(user, syncDate);
        Set<String> existingUids = heartRates.stream().map(HeartRate::getUid).collect(Collectors.toSet());

        // 삭제된 uid 리스트
        Set<String> uidToDelete = existingUids.stream()
                .filter(uid -> !sdkUids.contains(uid))
                .collect(Collectors.toSet());

        // 삭제
        if(!uidToDelete.isEmpty()) {
            List<HeartRate> heartRateToDelete = heartRates.stream()
                            .filter(heartRate -> uidToDelete.contains(heartRate.getUid()))
                                    .toList();
            heartRateRepository.deleteAll(heartRateToDelete);
        }


        for (HeartRateDto dto : heartRateList) {
            Optional<HeartRate> existingData = heartRateRepository.findByUserAndUid(user, dto.getUid());

            // 심박수 수정 불가
            if (existingData.isPresent()) {
                continue;
            } else {
                heartRateRepository.save(HeartRate.builder()
                                .deviceId(dto.getDeviceId())
                                .deviceType(dto.getDeviceType())
                                .uid(dto.getUid())
                                .zoneOffset(dto.getZoneOffset())
                                .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                .dataSource(dto.getDataSource())
                                .heartRate(dto.getHeartRate())
                                .user(user)
                                .build()
                        );
            }
        }
    }
}
