package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.BloodPressureDto;
import com.ssafy.linkcare.health.entity.BloodPressure;
import com.ssafy.linkcare.health.repository.BloodPressureRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
                        .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
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

    @Transactional
    public void saveDailyBloodPressureData(List<BloodPressureDto> bloodPressureList, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if(bloodPressureList == null || bloodPressureList.isEmpty()) {
            return;
        }

        // 삼성 헬스 uids
        Set<String> sdkUids = bloodPressureList.stream().map(BloodPressureDto::getUid).collect(Collectors.toSet());

        // 동기화 날짜
        LocalDate syncDate = bloodPressureList.get(0).getStartTime().toLocalDate();

        // db에 저장되어 있는 혈압
        List<BloodPressure> bloodPressures = bloodPressureRepository.findByUserAndStartDate(user, syncDate);
        Set<String> existingUids = bloodPressures.stream().map(BloodPressure::getUid).collect(Collectors.toSet());

        // 삭제된 uid 리스트
        Set<String> uidToDelete = existingUids.stream()
                        .filter(uid -> !sdkUids.contains(uid))
                                .collect(Collectors.toSet());

        // 삭제
        if(!uidToDelete.isEmpty()) {
            bloodPressureRepository.deleteByUserAndUidIn(user, uidToDelete);
        }

        bloodPressureList.forEach(dto -> {
            Optional<BloodPressure> existingData = bloodPressureRepository.findByUserAndUid(user, dto.getUid());
            if(existingData.isPresent()) {
                // 수정 삭제 가능
                BloodPressure bloodPressure = existingData.get();
                bloodPressure.updateSystolicAndDiastolicAndPulseRate(dto.getSystolic(), dto.getDiastolic(), dto.getPulseRate());
            } else {
                bloodPressureRepository.save(BloodPressure.builder()
                        .deviceId(dto.getDeviceId())
                        .deviceType(dto.getDeviceType())
                        .uid(dto.getUid())
                        .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                        .dataSource(dto.getDataSource())
                        .systolic(dto.getSystolic())
                        .diastolic(dto.getDiastolic())
                        .mean(dto.getMean())
                        .pulseRate(dto.getPulseRate())
                        .user(user)
                        .build()
                );
            };
        });
    }
}
