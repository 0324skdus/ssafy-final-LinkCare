package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.*;
import com.ssafy.linkcare.health.entity.Sleep;
import com.ssafy.linkcare.health.entity.SleepSession;
import com.ssafy.linkcare.health.repository.SleepRepository;
import com.ssafy.linkcare.health.repository.SleepSessionRepository;
import com.ssafy.linkcare.health.util.TimeStampUtil;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SleepService {

    private final SleepRepository sleepRepository;
    private final SleepSessionRepository sleepSessionRepository;
    private final UserRepository userRepository;

    private final TimeStampUtil timeStampUtil;

    @Transactional
    public void saveAllSleepData(List<SleepDataDto> sleepList, int userSeq) {
        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        for (SleepDataDto dto : sleepList) {// 1. Sleep 저장
            Sleep savedSleep = sleepRepository.save(
                    Sleep.builder()
                            .deviceId(dto.getDeviceId())
                            .deviceType(dto.getDeviceType())
                            .uid(dto.getUid())
                            .zoneOffset(dto.getZoneOffset())
                            .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                            .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                            .dataSource(dto.getDataSource())
                            .duration(dto.getDuration())
                            .user(user)
                            .build()
            );

            // 2. Session 저장
            if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                List<SleepSession> sessions = dto.getSessions().stream()
                        .map(sessionDto -> SleepSession.builder()
                                .sleep(savedSleep)
                                .startTime(sessionDto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                .endTime(sessionDto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                .duration(sessionDto.getDuration())
                                .build())
                        .toList();

                sleepSessionRepository.saveAll(sessions);
            }
        }
    }

    @Transactional
    public void saveDailySleepData(List<SleepDataDto> sleepList, int userSeq) {
        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if(sleepList == null || sleepList.isEmpty()) {
            return;
        }

        // sleep 삭제 확인
        // 삼성헬스에서 받은 uid 리스트
        Set<String> sleepUids = sleepList.stream().map(SleepDataDto::getUid).collect(Collectors.toSet());
        LocalDate syncDate = sleepList.get(0).getEndTime().toLocalDate();

        // db 에 있는 수면 리스트
        List<Sleep> existingSleeps = sleepRepository.findByUserAndEndDate(user, syncDate);
        Set<String> existingUids = existingSleeps.stream().map(Sleep::getUid).collect(Collectors.toSet());

        // 삭제된 uid
        Set<String> uidsToDelete = existingUids.stream()
                .filter(uid -> !sleepUids.contains(uid))
                .collect(Collectors.toSet());

        if(!uidsToDelete.isEmpty()) {
            List<Sleep> sleepToDelete = existingSleeps.stream()
                            .filter(sleep -> uidsToDelete.contains(sleep.getUid())).toList();
            sleepRepository.deleteAll(sleepToDelete);
        }

        // 수정, 삭제 가능
        sleepList.forEach(dto -> {
            // 수면 데이터가 이미 있으면 교체
            Optional<Sleep> existingData = sleepRepository.findByUserAndUid(user, dto.getUid());
            if(existingData.isPresent()) {
                Sleep sleep = existingData.get();
                sleep.updateDuration(dto.getDuration());

                if(dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                    // 삼성헬스에서 받은 session 정보
                    Set<String> syncedSessionKeys = dto.getSessions().stream()
                            .map(session -> session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond()
                                    + "_" + session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                            .collect(Collectors.toSet());

                    // db에 저장된 session 정보
                    List<SleepSession> existingSessions = sleepSessionRepository.findBySleep(sleep);
                    Set<String> existingSessionKeys = existingSessions.stream()
                            .map(session -> session.getStartTime() + "_" + session.getEndTime())
                            .collect(Collectors.toSet());

                    // db에는 있지만 삼성헬스에는 없는 세션
                    Set<String> sessionKeysToDelete = existingSessionKeys.stream()
                            .filter(key -> !syncedSessionKeys.contains(key))
                            .collect(Collectors.toSet());

                    // 삭제된 세션들 삭제
                    if(!sessionKeysToDelete.isEmpty()) {
                        existingSessions.stream()
                                .filter(session -> sessionKeysToDelete.contains(
                                        session.getStartTime() + "_" + session.getEndTime()))
                                .forEach(sleepSessionRepository::delete);
                    }

                    dto.getSessions().forEach(sleepSessionDto -> {
                        Optional<SleepSession> existingSession = sleepSessionRepository.findBySleepAndStartTimeAndEndTime(sleep,
                                sleepSessionDto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond(),
                                sleepSessionDto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond());
                        if(existingSession.isPresent()) {
                            SleepSession session = existingSession.get();
                            session.updateDuration(sleepSessionDto.getDuration());
                        } else {
                            // 없으면 저장
                            sleepSessionRepository.save(SleepSession.builder()
                                    .startTime(sleepSessionDto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .endTime(sleepSessionDto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .duration(sleepSessionDto.getDuration())
                                    .build());
                        }
                    });
                } else {
                    sleepSessionRepository.deleteBySleep(sleep);
                }

                long remainingSessionCount = sleepSessionRepository.countBySleep(sleep);
                if(remainingSessionCount == 0) {
                    // 세션이 하나도 없으면 Sleep도 삭제
                    sleepRepository.delete(sleep);
                }
            }

            // 수면 데이터가 없다면 저장
            else {
                // 1. Sleep 저장
                Sleep savedSleep = sleepRepository.save(
                        Sleep.builder()
                                .deviceId(dto.getDeviceId())
                                .deviceType(dto.getDeviceType())
                                .uid(dto.getUid())
                                .zoneOffset(dto.getZoneOffset())
                                .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                .dataSource(dto.getDataSource())
                                .duration(dto.getDuration())
                                .user(user)
                                .build()
                );

                // 2. Session 저장
                if(dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                    List<SleepSession> sessions = dto.getSessions().stream()
                            .map(sessionDto -> SleepSession.builder()
                                    .sleep(savedSleep)
                                    .startTime(sessionDto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .endTime(sessionDto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .duration(sessionDto.getDuration())
                                    .build())
                            .toList();

                    sleepSessionRepository.saveAll(sessions);
                }
            }
        });
    }

    /**
     * 당일 수면 데이터 조회 (endTime 기준)
     * 오늘 일어난(종료된) 수면 데이터를 조회
     */
    public List<SleepResponse> getTodaySleeps(int userSeq) {
        User user = validateUser(userSeq);

        Long dayStart = timeStampUtil.getTodayStartTimestamp();
        Long dayEnd = timeStampUtil.getCurrentTimestamp();

        log.info("당일 수면 데이터 조회 - User: {}, Start: {}, End: {}", userSeq, dayStart, dayEnd);

        // endTime을 기준으로 조회
        List<Sleep> sleeps = sleepRepository
                .findByUserAndEndTimeBetweenOrderByEndTimeDesc(user, dayStart, dayEnd);

        return sleeps.stream()
                .map(this::convertToSleepResponse)
                .collect(Collectors.toList());
    }

    /**
     * 이번 주 수면 데이터 조회 (endTime 기준)
     * 이번 주에 일어난(종료된) 수면 데이터를 조회
     * @param userSeq 사용자 시퀀스
     * @return SleepResponse 리스트
     */
    public List<SleepResponse> getThisWeekSleeps(int userSeq) {
        User user = validateUser(userSeq);

        Long weekStart = timeStampUtil.getThisWeekStartTimestamp(); // 월요일 00:00
        Long now = timeStampUtil.getCurrentTimestamp();

        log.info("이번 주 수면 데이터 조회 (endTime 기준) - User: {}", userSeq);

        // endTime이 이번 주에 속하는 수면 조회
        List<Sleep> sleeps = sleepRepository
                .findByUserAndEndTimeBetweenOrderByEndTimeDesc(user, weekStart, now);

        return sleeps.stream()
                .map(this::convertToSleepResponse)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 수면 데이터 조회
     * @param userSeq 사용자 시퀀스
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return SleepResponse 리스트
     */
     public List<SleepResponse> getSleepsByPeriod(int userSeq, Long startTime, Long endTime) {
         User user = validateUser(userSeq);

         List<Sleep> sleeps = sleepRepository
                 .findByUserAndEndTimeBetweenOrderByEndTimeDesc(user, startTime, endTime);

         return sleeps.stream()
                 .map(this::convertToSleepResponse)
                 .collect(Collectors.toList());
     }

    /**
     * 기간별 수면 데이터 조회
     * @param userSeq 사용자 시퀀스
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return SleepResponse 리스트
     */
    public List<SleepResponse> getSleepsByDate(int userSeq, LocalDate startTime, LocalDate endTime) {
        User user = validateUser(userSeq);

        Long startTimeTs = timeStampUtil.getDateStartTimestamp(startTime);
        Long endTimeTs = timeStampUtil.getDateEndTimestamp(endTime);

        List<Sleep> sleeps = sleepRepository
                .findByUserAndEndTimeBetweenOrderByEndTimeDesc(user, startTimeTs, endTimeTs);

        return sleeps.stream()
                .map(this::convertToSleepResponse)
                .collect(Collectors.toList());
    }


    /**
     * 3주치 수면 데이터 조회
     * @param userSeq 사용자 시퀀스
     * @return SleepResponse 리스트
     */
    public List<SleepResponse> getThreeWeeksSleeps(int userSeq) {
        User user = validateUser(userSeq);

        Long threeWeeksAgo = timeStampUtil.getThreeWeeksAgoTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();

        log.info("3주치 수면 데이터 조회 - User: {}", userSeq);

        List<Sleep> sleeps = sleepRepository
                .findByUserAndEndTimeBetweenOrderByEndTimeDesc(user, threeWeeksAgo, now);

        return sleeps.stream()
                .map(this::convertToSleepResponse)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 수면 통계 조회
     * @param userSeq 사용자 시퀀스
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 수면 통계
     */
    public SleepStatisticsResponse getSleepStatistics(
            int userSeq,
            Long startTime,
            Long endTime
    ) {
        User user = validateUser(userSeq);

        Long effectiveStartTime = startTime != null ? startTime : timeStampUtil.getThreeWeeksAgoTimestamp();
        Long effectiveEndTime = endTime != null ? endTime : timeStampUtil.getCurrentTimestamp();

        log.info("수면 통계 조회 - User: {}, Start: {}, End: {}", userSeq, effectiveStartTime, effectiveEndTime);

        Long totalDuration = sleepRepository
                .sumTotalDurationByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0L);

        Double avgDuration = sleepRepository
                .avgDurationByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0.0);

        Integer maxDuration = sleepRepository
                .maxDurationByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0);

        Integer minDuration = sleepRepository
                .minDurationByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0);

        return SleepStatisticsResponse.builder()
                .totalDuration(totalDuration)
                .averageDuration(avgDuration)
                .maxDuration(maxDuration)
                .minDuration(minDuration)
                .startTime(timeStampUtil.toLocalDateTime(effectiveStartTime))
                .endTime(timeStampUtil.toLocalDateTime(effectiveEndTime))
                .build();
    }

    /**
     *  (LocalDate) 기간별 수면 통계 조회
     * @param userSeq 사용자 시퀀스
     * @param startDate 시작 시간
     * @param endDate 종료 시간
     * @return 수면 통계
     */
    public SleepStatisticsResponse getSleepStatisticsByDate(
            int userSeq,
            LocalDate startDate,
            LocalDate endDate
    ) {
        User user = validateUser(userSeq);

        Long startDateTs = timeStampUtil.getDateStartTimestamp(startDate);
        Long endDateTs = timeStampUtil.getDateEndTimestamp(endDate);

        log.info("수면 통계 조회 - User: {}, Start: {}, End: {}", userSeq, startDateTs, endDateTs);

        Long totalDuration = sleepRepository
                .sumTotalDurationByUserAndPeriod(user, startDateTs, endDateTs)
                .orElse(0L);

        Double avgDuration = sleepRepository
                .avgDurationByUserAndPeriod(user, startDateTs, endDateTs)
                .orElse(0.0);

        Integer maxDuration = sleepRepository
                .maxDurationByUserAndPeriod(user, startDateTs, endDateTs)
                .orElse(0);

        Integer minDuration = sleepRepository
                .minDurationByUserAndPeriod(user, startDateTs, endDateTs)
                .orElse(0);

        return SleepStatisticsResponse.builder()
                .totalDuration(totalDuration)
                .averageDuration(avgDuration)
                .maxDuration(maxDuration)
                .minDuration(minDuration)
                .startTime(timeStampUtil.toLocalDateTime(startDateTs))
                .endTime(timeStampUtil.toLocalDateTime(endDateTs))
                .build();
    }

    /**
     * 당일 수면 통계 조회
     */
    public SleepStatisticsResponse getTodaySleepStatistics(int userSeq) {
        Long dayStart = timeStampUtil.getTodayStartTimestamp();
        Long dayEnd = timeStampUtil.getCurrentTimestamp();
        return getSleepStatistics(userSeq, dayStart, dayEnd);
    }

    /**
     * 이번 주 수면 통계 조회
     */
    public SleepStatisticsResponse getThisWeekSleepStatistics(int userSeq) {
        Long weekStart = timeStampUtil.getThisWeekStartTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();
        return getSleepStatistics(userSeq, weekStart, now);
    }

    /**
     * 기간별 수면 시간 조회
     * @param userSeq 사용자 시퀀스
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return SleepResponse 리스트
     */
    public List<Long> getTotalSleepDurationByDate(int userSeq, LocalDate startDate, LocalDate endDate) {
        User user = validateUser(userSeq);

        Long startTimeTs = timeStampUtil.getDateStartTimestamp(startDate);
        Long endTimeTs = timeStampUtil.getDateEndTimestamp(endDate);

        List<Sleep> sleeps = sleepRepository
                .findByUserAndEndTimeBetweenOrderByEndTimeDesc(user, startTimeTs, endTimeTs);

        return sleeps.stream()
                .map(sleep -> sleep.getDuration() != 0 ? sleep.getDuration() : 0L)
                .toList();
    }

    // 기간별 수면 시간, 평균 수면시간, 총 수면시간 조회
    public SleepDetailResponse getSleepStatisticsForGroup(int userSeq, LocalDate startDate, LocalDate endDate) {
        User user = validateUser(userSeq);

        Long startTimeTs = timeStampUtil.getDateStartTimestamp(startDate);
        Long endTimeTs = timeStampUtil.getDateEndTimestamp(endDate);

        List<Sleep> sleeps = sleepRepository
                .findByUserAndEndTimeBetweenOrderByEndTimeAsc(user, startTimeTs, endTimeTs);

        // 날짜별 수면시간 맵 생성 (같은 날 여러 기록 있으면 합산)
        Map<LocalDate, Long> sleepByDate = sleeps.stream()
                .collect(Collectors.groupingBy(
                        sleep -> timeStampUtil.toLocalDate(sleep.getEndTime()),
                        Collectors.summingLong(Sleep::getDuration)
                ));

        // 기간 내 모든 날짜에 대해 수면시간 리스트 생성 (없으면 0)
        List<Long> dailySleepMinutes = startDate.datesUntil(endDate.plusDays(1))
                .map(date -> sleepByDate.getOrDefault(date, 0L))
                .collect(Collectors.toList());

        // 총 수면시간
        Long totalSleepMinutes = dailySleepMinutes.stream()
                .filter(Objects::nonNull)  // null 제거
                .mapToLong(duration -> duration)  // 람다로 변경
                .sum();

        // 평균 수면시간
        Long averageSleepMinutes = dailySleepMinutes.isEmpty() ? 0L :
                totalSleepMinutes / dailySleepMinutes.size();

        return SleepDetailResponse.builder()
                .userSeq(userSeq)
                .dailySleepMinutes(dailySleepMinutes)
                .totalSleepMinutes(totalSleepMinutes)
                .averageSleepMinutes(averageSleepMinutes)
                .build();
    }

    private SleepResponse convertToSleepResponse(Sleep sleep) {
        // SleepSession을 SleepSessionResponse로 변환
        List<SleepSessionResponse> sessionResponses = sleep.getSessions().stream()
                .map(this::convertToSleepSessionResponse)
                .collect(Collectors.toList());

        return SleepResponse.builder()
                .sleepId(sleep.getSleepId())
//                .deviceId(sleep.getDeviceId())
//                .deviceType(sleep.getDeviceType())
//                .uid(sleep.getUid())
                .startTime(timeStampUtil.toLocalDateTime(sleep.getStartTime()))
                .endTime(timeStampUtil.toLocalDateTime(sleep.getEndTime()))
                .duration(sleep.getDuration())
                .sessions(sessionResponses)
                .build();
    }

    private SleepSessionResponse convertToSleepSessionResponse(SleepSession sleepSession) {
        return SleepSessionResponse.builder()
                .sleepSessionId(sleepSession.getSleepSessionId())
                .startTime(timeStampUtil.toLocalDateTime(sleepSession.getStartTime()))
                .endTime(timeStampUtil.toLocalDateTime(sleepSession.getEndTime()))
                .duration(sleepSession.getDuration())
                .build();
    }
    private User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
