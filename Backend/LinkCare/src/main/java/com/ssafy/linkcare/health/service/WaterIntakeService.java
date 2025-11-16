package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.WaterIntakeDto;
import com.ssafy.linkcare.health.dto.WaterIntakeResponse;
import com.ssafy.linkcare.health.dto.WaterIntakeStatisticsResponse;
import com.ssafy.linkcare.health.entity.WaterIntake;
import com.ssafy.linkcare.health.repository.WaterIntakeRepository;
import com.ssafy.linkcare.health.util.TimeStampUtil;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.core.Local;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaterIntakeService {

    private final UserRepository userRepository;
    private final WaterIntakeRepository waterIntakeRepository;

    private final TimeStampUtil timeStampUtil;

    @Transactional
    public void saveAllWaterIntakeData(List<WaterIntakeDto> waterIntakeList, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        waterIntakeList.forEach(waterIntakeDto -> {

            if(waterIntakeDto.getWaterIntakes() == null || waterIntakeDto.getWaterIntakes().isEmpty()) {
                LocalDate today = LocalDate.now();
                List<WaterIntake> existingWaterIntake = waterIntakeRepository.findByUserAndStartDate(user, today);
                if(existingWaterIntake.isEmpty()) {
                    waterIntakeRepository.save(WaterIntake.builder()
                            .deviceId("system")
                            .deviceType("auto-generated")
                            .uid("empty_" + System.currentTimeMillis())
                            .amount(0)
                            .zoneOffset(ZoneId.systemDefault().getId())
                            .dataSource(null)
                            .startTime(today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond())
                            .user(user)
                            .goal(0)
                            .build()
                    );
                }
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
        if(waterIntakeDto == null || waterIntakeDto.getGoal() == 0 &&(waterIntakeDto.getWaterIntakes() == null || waterIntakeDto.getWaterIntakes().isEmpty())) {
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

    /**
     * 당일 물 섭취 데이터 조회
     * @param userSeq 사용자 시퀀스
     * @return WaterIntakeGroupedResponse 리스트
     */
    public List<WaterIntakeResponse> getTodayWaterIntakes(int userSeq) {
        User user = validateUser(userSeq);

        Long dayStart = timeStampUtil.getTodayStartTimestamp();
        Long dayEnd = timeStampUtil.getCurrentTimestamp();

        log.info("당일 물 섭취 데이터 조회 - User: {}, Start: {}, End: {}", userSeq, dayStart, dayEnd);

        List<WaterIntake> waterIntakes = waterIntakeRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, dayStart, dayEnd);

        return waterIntakes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 이번 주 물 섭취 데이터 조회
     * @param userSeq 사용자 시퀀스
     * @return WaterIntakeGroupedResponse 리스트
     */
    public List<WaterIntakeResponse> getThisWeekWaterIntakes(int userSeq) {
        User user = validateUser(userSeq);

        Long weekStart = timeStampUtil.getThisWeekStartTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();

        log.info("이번 주 물 섭취 데이터 조회 - User: {}", userSeq);

        List<WaterIntake> waterIntakes = waterIntakeRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, weekStart, now);

        return waterIntakes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 3주치 물 섭취 데이터 조회
     * @param userSeq 사용자 시퀀스
     * @return WaterIntakeGroupedResponse 리스트
     */
    public List<WaterIntakeResponse> getThreeWeeksWaterIntakes(int userSeq) {
        User user = validateUser(userSeq);

        Long threeWeeksAgo = timeStampUtil.getThreeWeeksAgoTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();

        log.info("3주치 물 섭취 데이터 조회 - User: {}", userSeq);

        List<WaterIntake> waterIntakes = waterIntakeRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, threeWeeksAgo, now);

        return waterIntakes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 물 섭취 데이터 조회
     * @param userSeq 사용자 시퀀스
     * @return WaterIntakeGroupedResponse 리스트
     */
    public List<WaterIntakeResponse> getWaterIntakeByDate(int userSeq, LocalDate startDate, LocalDate endDate) {
        User user = validateUser(userSeq);

        Long startDateTs = timeStampUtil.getDateStartTimestamp(startDate);
        Long endDateTs = timeStampUtil.getDateEndTimestamp(endDate);

        log.info("기간별 물 섭취 데이터 조회 - User: {}", userSeq);

        List<WaterIntake> waterIntakes = waterIntakeRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, startDateTs, endDateTs);

        return waterIntakes.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 물 섭취 통계 조회
     * @param userSeq 사용자 시퀀스
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 물 섭취 통계
     */
    public WaterIntakeStatisticsResponse getWaterIntakeStatistics(
            int userSeq,
            Long startTime,
            Long endTime
    ) {
        User user = validateUser(userSeq);

        Long effectiveStartTime = startTime != null ? startTime : timeStampUtil.getThreeWeeksAgoTimestamp();
        Long effectiveEndTime = endTime != null ? endTime : timeStampUtil.getCurrentTimestamp();

        log.info("물 섭취 통계 조회 - User: {}, Start: {}, End: {}", userSeq, effectiveStartTime, effectiveEndTime);

        Float totalAmount = waterIntakeRepository
                .sumTotalAmountByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0.0f);

        Float avgAmount = waterIntakeRepository
                .avgAmountByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0.0f);

        Long intakeCount = waterIntakeRepository
                .countByUserAndPeriod(user, effectiveStartTime, effectiveEndTime);

        // 최신 목표량 조회
        Float dailyGoal = waterIntakeRepository
                .findLatestGoalByUser(user)
                .orElse(0.0f);


        return WaterIntakeStatisticsResponse.builder()
                .totalAmount(totalAmount)
                .averageAmount(avgAmount)
                .dailyGoal(dailyGoal)
                .intakeCount(intakeCount)
                .startTime(timeStampUtil.toLocalDateTime(effectiveStartTime))
                .endTime(timeStampUtil.toLocalDateTime(effectiveEndTime))
                .build();
    }

    public WaterIntakeStatisticsResponse getWaterIntakeStatisticsByDate(
            int userSeq,
            LocalDate startTime,
            LocalDate endTime
    ) {
        User user = validateUser(userSeq);

        Long startDateTs = timeStampUtil.getDateStartTimestamp(startTime);
        Long endDateTs = timeStampUtil.getDateEndTimestamp(endTime);

        log.info("(LocalDate) 물 섭취 통계 조회 - User: {}, Start: {}, End: {}", userSeq, startDateTs, endDateTs);

        Float totalAmount = waterIntakeRepository
                .sumTotalAmountByUserAndPeriod(user, startDateTs, endDateTs)
                .orElse(0.0f);

        Float avgAmount = waterIntakeRepository
                .avgAmountByUserAndPeriod(user, startDateTs, endDateTs)
                .orElse(0.0f);

        Long intakeCount = waterIntakeRepository
                .countByUserAndPeriod(user, startDateTs, endDateTs);

        // 최신 목표량 조회
        Float dailyGoal = waterIntakeRepository
                .findLatestGoalByUser(user)
                .orElse(0.0f);


        return WaterIntakeStatisticsResponse.builder()
                .totalAmount(totalAmount)
                .averageAmount(avgAmount)
                .dailyGoal(dailyGoal)
                .intakeCount(intakeCount)
                .startTime(timeStampUtil.toLocalDateTime(startDateTs))
                .endTime(timeStampUtil.toLocalDateTime(endDateTs))
                .build();
    }

    /**
     * 당일 물 섭취 통계 조회 (목표 달성률 포함)
     */
    public WaterIntakeStatisticsResponse getTodayWaterIntakeStatistics(int userSeq) {
        User user = validateUser(userSeq);

        Long dayStart = timeStampUtil.getTodayStartTimestamp();
        Long dayEnd = timeStampUtil.getCurrentTimestamp();

        log.info("당일 물 섭취 통계 조회 - User: {}", userSeq);

        Float totalAmount = waterIntakeRepository
                .sumTotalAmountByUserAndPeriod(user, dayStart, dayEnd)
                .orElse(0.0f);

        Float avgAmount = waterIntakeRepository
                .avgAmountByUserAndPeriod(user, dayStart, dayEnd)
                .orElse(0.0f);

        Long intakeCount = waterIntakeRepository
                .countByUserAndPeriod(user, dayStart, dayEnd);

        // 당일 최신 목표량 조회
        Float dailyGoal = waterIntakeRepository
                .findLatestGoalByUser(user)
                .orElse(0.0f);

        // 당일 목표 달성률 계산
        Float goalAchievementRate = 0.0f;
        if (dailyGoal > 0) {
            goalAchievementRate = (totalAmount / dailyGoal) * 100;
        }

        log.info("당일 총 음수량 - amount: {}", totalAmount);

        return WaterIntakeStatisticsResponse.builder()
                .totalAmount(totalAmount)
                .averageAmount(avgAmount)
                .dailyGoal(dailyGoal)
                .goalAchievementRate(goalAchievementRate)
                .intakeCount(intakeCount)
                .startTime(timeStampUtil.toLocalDateTime(dayStart))
                .endTime(timeStampUtil.toLocalDateTime(dayEnd))
                .build();
    }

    /**
     * 이번 주 물 섭취 통계 조회
     */
    public WaterIntakeStatisticsResponse getThisWeekWaterIntakeStatistics(int userSeq) {
        Long weekStart = timeStampUtil.getThisWeekStartTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();
        return getWaterIntakeStatistics(userSeq, weekStart, now);
    }

    /**
     * 3주치 물 섭취 통계 조회
     */
    public WaterIntakeStatisticsResponse getThreeWeeksWaterIntakeStatistics(int userSeq) {
        Long threeWeeksAgo = timeStampUtil.getThreeWeeksAgoTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();
        return getWaterIntakeStatistics(userSeq, threeWeeksAgo, now);
    }

    private User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private WaterIntakeResponse convertToResponse(WaterIntake waterIntake) {
        return WaterIntakeResponse.builder()
                .waterIntakeId(waterIntake.getWaterIntakeId())
//                .deviceId(waterIntake.getDeviceId())
//                .deviceType(waterIntake.getDeviceType())
//                .uid(waterIntake.getUid())
                .startTime(timeStampUtil.toLocalDateTime(waterIntake.getStartTime()))
                .amount(waterIntake.getAmount())
                .goal(waterIntake.getGoal())
                .build();
    }
}

