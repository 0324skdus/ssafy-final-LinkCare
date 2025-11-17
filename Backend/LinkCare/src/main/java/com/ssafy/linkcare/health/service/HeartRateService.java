package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.*;
import com.ssafy.linkcare.health.entity.HeartRate;
import com.ssafy.linkcare.health.repository.HeartRateRepository;
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
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HeartRateService {

    private final UserRepository userRepository;
    private final HeartRateRepository heartRateRepository;
    private final TimeStampUtil timeStampUtil;

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

    public List<HeartRateResponse> getHeartRatesByPeriod(int userSeq, Long startTime, Long endTime) {
        User user = validateUser(userSeq);

        // 기본값 3주
        Long startTimeTs = timeStampUtil.getThreeWeeksAgoTimestamp();
        Long endTimeTs = timeStampUtil.getCurrentTimestamp();

        log.info("기간별 조회 - User: {}, Start: {}, End: {}",
                userSeq, startTimeTs, endTimeTs);

        List<HeartRate> steps = heartRateRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user,startTimeTs, endTimeTs);

        return steps.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    public List<HeartRateResponse> getHeartRatesByPeriod(int userSeq, LocalDate startTime, LocalDate endTime) {
        User user = validateUser(userSeq);

        Long startTimeTs = timeStampUtil.getDateStartTimestamp(startTime);
        Long endTimeTs = timeStampUtil.getDateEndTimestamp(endTime);

        log.info("(LocalDate) 특정 기간별 조회 - User: {}, Start: {}, End: {}",
                userSeq, startTimeTs, endTimeTs);

        List<HeartRate> steps = heartRateRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user,startTimeTs, endTimeTs);

        return steps.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 당일 데이터 조회
    public List<HeartRateResponse> getTodayHeartRate(int userSeq) {
        User user = validateUser(userSeq);

        Long startTimeTs = timeStampUtil.getTodayStartTimestamp();
        Long endTimeTs = timeStampUtil.getCurrentTimestamp();

        List<HeartRate> heartRates = heartRateRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, startTimeTs, endTimeTs);

        return  heartRates.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    // 일주일 데이터 조회
    public List<HeartRateResponse> getThisWeekHeartRates(int userSeq) {
        User user = validateUser(userSeq);

        Long weekStart = timeStampUtil.getThisWeekStartTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();

        log.info("이번 주 데이터 조회 - User: {}", userSeq);

        return getHeartRatesByPeriod(userSeq, weekStart, now);
    }


    private HeartRateResponse convertToResponse(HeartRate heartRate) {
        return HeartRateResponse.builder()
                .heartRateId(heartRate.getHeartRateId())
//                .uid(heartRate.getUid())
                .startTime(timeStampUtil.toLocalDateTime(heartRate.getStartTime()))
                .endTime(timeStampUtil.toLocalDateTime(heartRate.getEndTime()))
//                .deviceId(heartRate.getDeviceId())
//                .deviceType(heartRate.getDeviceType())
                .heartRate(heartRate.getHeartRate())
                .build();
    }

    /**
     * 기간별 심박수 통계 조회
     */
    public HeartRateStaticsResponse getHeartRateStatsByDate(int userSeq, LocalDate startDate, LocalDate endDate) {
        User user = validateUser(userSeq);

        long startTimestamp = timeStampUtil.getDateStartTimestamp(startDate);
        long endTimestamp = timeStampUtil.getDateEndTimestamp(endDate);

        Double avgHeartRate = heartRateRepository.findAverageHeartRateByPeriod(user, startTimestamp, endTimestamp);
        Double maxHeartRate = heartRateRepository.findMaxHeartRateByPeriod(user, startTimestamp, endTimestamp);
        Double minHeartRate = heartRateRepository.findMinHeartRateByPeriod(user, startTimestamp, endTimestamp);

        return HeartRateStaticsResponse.builder()
                .startTime(startDate)
                .endTime(endDate)
                .avgHeartRate(avgHeartRate != null ? avgHeartRate : 0.0)
                .maxHeartRate(maxHeartRate != null ? maxHeartRate : 0.0)
                .minHeartRate(minHeartRate != null ? minHeartRate : 0.0)
                .build();
    }

    private User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
