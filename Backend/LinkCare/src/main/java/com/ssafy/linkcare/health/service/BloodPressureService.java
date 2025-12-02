package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.BloodPressureDto;
import com.ssafy.linkcare.health.dto.BloodPressureResponse;
import com.ssafy.linkcare.health.dto.BloodPressureStaticsResponse;
import com.ssafy.linkcare.health.entity.BloodPressure;
import com.ssafy.linkcare.health.entity.Exercise;
import com.ssafy.linkcare.health.repository.BloodPressureRepository;
import com.ssafy.linkcare.health.util.TimeStampUtil;
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

    private final TimeStampUtil timeStampUtil;

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
            List<BloodPressure> bloodPressuresToDelete = bloodPressures.stream()
                            .filter(bloodPressure -> uidToDelete.contains(bloodPressure.getUid()))
                                    .toList();
            bloodPressureRepository.deleteAll(bloodPressuresToDelete);
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

    // 당일 데이터 조회
    public List<BloodPressureResponse> getTodayBloodPressures(int userSeq) {
        User user = validateUser(userSeq);

        Long startTimeTs = timeStampUtil.getTodayStartTimestamp();
        Long endTimeTs = timeStampUtil.getCurrentTimestamp();

        List<BloodPressure> bloodPressures = bloodPressureRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, startTimeTs, endTimeTs);

        return bloodPressures.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public List<BloodPressureResponse> getBloodPressureByDate(int userSeq, LocalDate startDate, LocalDate endDate) {
        User user = validateUser(userSeq);

        Long startTimeTs = timeStampUtil.getDateStartTimestamp(startDate);
        Long endTimeTs = timeStampUtil.getDateEndTimestamp(endDate);

        List<BloodPressure> bloodPressures = bloodPressureRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, startTimeTs, endTimeTs);

        return bloodPressures.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private BloodPressureResponse convertToResponse(BloodPressure bloodPressure) {
        if(bloodPressure == null) return null;

        return BloodPressureResponse.builder()
                .bloodPressureId(bloodPressure.getBloodPressureId())
                .uid(bloodPressure.getUid())
//                .deviceId(bloodPressure.getDeviceId())
//                .deviceType(bloodPressure.getDeviceType())
                .startTime(timeStampUtil.toLocalDateTime(bloodPressure.getStartTime()))
                .systolic(bloodPressure.getSystolic())
                .diastolic(bloodPressure.getDiastolic())
                .mean(bloodPressure.getMean())
                .pulseRate(bloodPressure.getPulseRate())
                .build();
    }

    /**
     * 기간별 혈압 통계 조회
     */
    public BloodPressureStaticsResponse getBloodPressureStatsByPeriod(int userSeq, LocalDate startDate, LocalDate endDate) {
        User user = validateUser(userSeq);

        long startTimestamp = timeStampUtil.getDateStartTimestamp(startDate);
        long endTimestamp = timeStampUtil.getDateEndTimestamp(endDate);

        Double avgSystolic = bloodPressureRepository.findAverageSystolicByPeriod(user, startTimestamp, endTimestamp);
        Double avgDiastolic = bloodPressureRepository.findAverageDiastolicByPeriod(user, startTimestamp, endTimestamp);

        // 최고값
        Double maxSystolic = bloodPressureRepository.findMaxSystolicByPeriod(user, startTimestamp, endTimestamp);
        Double maxDiastolic = bloodPressureRepository.findMaxDiastolicByPeriod(user, startTimestamp, endTimestamp);

        // 최저값
        Double minSystolic = bloodPressureRepository.findMinSystolicByPeriod(user, startTimestamp, endTimestamp);
        Double minDiastolic = bloodPressureRepository.findMinDiastolicByPeriod(user, startTimestamp, endTimestamp);


        return BloodPressureStaticsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .avgSystolic(avgSystolic != null ? avgSystolic : 0.0)
                .avgDiastolic(avgDiastolic != null ? avgDiastolic : 0.0)
                .maxSystolic(maxSystolic != null ? maxSystolic : 0.0)
                .maxDiastolic(maxDiastolic != null ? maxDiastolic : 0.0)
                .minSystolic(minSystolic != null ? minSystolic : 0.0)
                .minDiastolic(minDiastolic != null ? minDiastolic : 0.0)
                .build();
    }

    public BloodPressureResponse getLastBloodPressureByDate(int userSeq, LocalDate startDate, LocalDate endDate) {
        User user = validateUser(userSeq);

        Long startDateTs = timeStampUtil.getDateStartTimestamp(startDate);
        Long endDateTs = timeStampUtil.getDateEndTimestamp(endDate);

        BloodPressure bloodPressure = bloodPressureRepository.findFirstByUserAndStartTimeBetweenOrderByStartTimeDesc(
                user, startDateTs, endDateTs).orElse(null);

        return convertToResponse(bloodPressure);
    }

    private User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
