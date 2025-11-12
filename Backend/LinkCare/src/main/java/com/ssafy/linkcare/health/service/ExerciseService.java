package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.*;
import com.ssafy.linkcare.health.entity.Exercise;
import com.ssafy.linkcare.health.entity.ExerciseSession;
import com.ssafy.linkcare.health.repository.ExerciseRepository;
import com.ssafy.linkcare.health.repository.ExerciseSessionRepository;
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
public class ExerciseService {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseSessionRepository exerciseSessionRepository;

    private final TimeStampUtil timeStampUtil;

    @Transactional
    public void saveAllExerciseData(List<ExerciseDto> exerciseList, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (exerciseList == null || exerciseList.isEmpty()) {
            return;
        }

        exerciseList.forEach(exerciseDto -> {
            if (exerciseDto.getExercises() == null || exerciseDto.getExercises().isEmpty()) {
                return;
            }
            for (ExerciseTypeDto dto : exerciseDto.getExercises()) {
                Optional<Exercise> existingData = exerciseRepository.findByUserAndUid(user, dto.getUid());
                if (existingData.isPresent()) {
                    continue;
                } else {
                    Exercise saveExercise = exerciseRepository.save(
                            Exercise.builder()
                                    .deviceId(dto.getDeviceId())
                                    .deviceType(dto.getDeviceType())
                                    .uid(dto.getUid())
                                    .zoneOffset(dto.getZoneOffset())
                                    .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .dataSource(dto.getDataSource())
                                    .exerciseType(dto.getExerciseType())
                                    .user(user)
                                    .build()
                    );

                    if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                        List<ExerciseSession> sessions = dto.getSessions().stream()
                                .map(sessionDto -> ExerciseSession.builder()
                                        .exercise(saveExercise)
                                        .startTime(sessionDto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                        .endTime(sessionDto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                        .exerciseType(sessionDto.getExerciseType())
                                        .calories(sessionDto.getCalories())
                                        .distance(sessionDto.getDistance())
                                        .duration(sessionDto.getDuration())
                                        .build()
                                ).toList();

                        exerciseSessionRepository.saveAll(sessions);
                    }
                }
            }
        });
    }

    @Transactional
    public void saveDailyExerciseData(ExerciseDto exerciseDto, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (exerciseDto.getExercises() == null || exerciseDto.getExercises().isEmpty()) {
            return;
        }
        // 삭제만 가능
        Set<String> sdkUids = exerciseDto.getExercises().stream().map(ExerciseTypeDto::getUid).collect(Collectors.toSet());

        // 동기화 날짜
        LocalDate syncDate = exerciseDto.getExercises().get(0).getStartTime().toLocalDate();

        // db 에 있는 uid
        List<Exercise> existingExercises = exerciseRepository.findByUserAndStartDate(user, syncDate);
        Set<String> existingUids = existingExercises.stream().map(Exercise::getUid).collect(Collectors.toSet());

        // 삭제된 uid 리스트들
        Set<String> uidToDelete = existingUids.stream().filter(uid -> !sdkUids.contains(uid)).collect(Collectors.toSet());

        // 삭제
        if (!uidToDelete.isEmpty()) {
            List<Exercise> exerciseToDelete = existingExercises.stream()
                    .filter(exercise -> uidToDelete.contains(exercise.getUid()))
                    .toList();
            exerciseRepository.deleteAll(exerciseToDelete);
        }

        for (ExerciseTypeDto dto : exerciseDto.getExercises()) {

            long startTimeTs = dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond();
            long endTimeTs = dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond();

            Optional<Exercise> existingData = exerciseRepository.findByUserAndUid(user, dto.getUid());
            if (existingData.isPresent()) {
                Exercise exercise = existingData.get();
                // 삭제된 세션이 있는지 확인
                if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                    Set<String> sdkSessionKeys = dto.getSessions().stream().map(session ->
                            session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond() + "_"
                                    + session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond()
                    ).collect(Collectors.toSet());

                    List<ExerciseSession> existingSessions = exerciseSessionRepository.findByExercise(exercise);
                    Set<String> existingSessionKeys = existingSessions.stream().map(
                            session -> session.getStartTime() + "_"
                                    + session.getEndTime()).collect(Collectors.toSet());

                    // 삭제된 session 확인
                    Set<String> sessionToDelete = existingSessionKeys.stream().filter(key -> !sdkSessionKeys.contains(key))
                            .collect(Collectors.toSet());

                    // 삭제
                    if (!sessionToDelete.isEmpty()) {
                        existingSessions.stream()
                                .filter(session -> sessionToDelete.contains(
                                        session.getStartTime() + "_" + session.getEndTime()))
                                .forEach(exerciseSessionRepository::delete);
                    }
                } else {
                    // 세션이 비어있다면
                    exerciseSessionRepository.deleteByExercise(exercise);
                }

                long remainingSessionCount = exerciseSessionRepository.countByExercise(exercise);
                if (remainingSessionCount == 0) {
                    // 세션이 하나도 없으면 Exercise도 삭제
                    exerciseRepository.delete(exercise);

                } else {
                    Exercise savedExercise = Exercise.builder()
                            .deviceId(dto.getDeviceId())
                            .deviceType(dto.getDeviceType())
                            .uid(dto.getUid())
                            .exerciseType(dto.getExerciseType())
                            .zoneOffset(dto.getZoneOffset())
                            .dataSource(dto.getDataSource())
                            .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                            .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                            .user(user)
                            .build();

                    exerciseRepository.save(savedExercise);

                    if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                        List<ExerciseSession> sessions = dto.getSessions().stream()
                                .map(session -> ExerciseSession.builder()
                                        .startTime(session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                        .endTime(session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                        .exerciseType(session.getExerciseType())
                                        .distance(session.getDistance())
                                        .calories(session.getCalories())
                                        .duration(session.getDuration())
                                        .exercise(savedExercise)
                                        .build()
                                ).toList();

                        exerciseSessionRepository.saveAll(sessions);
                    }
                }

            } else {
                Exercise newExercise = Exercise.builder()
                        .deviceId(dto.getDeviceId())
                        .deviceType(dto.getDeviceType())
                        .uid(dto.getUid())
                        .exerciseType(dto.getExerciseType())
                        .zoneOffset(dto.getZoneOffset())
                        .dataSource(dto.getDataSource())
                        .startTime(startTimeTs)
                        .endTime(endTimeTs)
                        .user(user)
                        .build();

                Exercise savedExercise = exerciseRepository.save(newExercise);
                log.info("새 운동 데이터 저장 - User: {}, UID: {}", userSeq, dto.getUid());

                // 세션 저장
                if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                    List<ExerciseSession> sessions = dto.getSessions().stream()
                            .map(session -> ExerciseSession.builder()
                                    .startTime(session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .endTime(session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                    .exerciseType(session.getExerciseType())
                                    .distance(session.getDistance())
                                    .calories(session.getCalories())
                                    .duration(session.getDuration())
                                    .exercise(savedExercise)
                                    .build()
                            ).toList();

                    exerciseSessionRepository.saveAll(sessions);
                    log.info("운동 세션 저장 - Exercise UID: {}, Sessions: {}", dto.getUid(), sessions.size());
                }
            }
        }

    }
    /**
     * 당일 운동 데이터 조회
     * @param userSeq 사용자 시퀀스
     * @return ExerciseTypeResponse 리스트
     */
    public List<ExerciseResponse> getTodayExercises(int userSeq) {
        User user = validateUser(userSeq);

        Long dayStart = timeStampUtil.getTodayStartTimestamp();
        Long dayEnd = timeStampUtil.getCurrentTimestamp();

        log.info("당일 운동 데이터 조회 - User: {}, Start: {}, End: {}", userSeq, dayStart, dayEnd);

        List<Exercise> exercises = exerciseRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, dayStart, dayEnd);

        return exercises.stream()
                .map(this::convertToExerciseTypeResponse)
                .collect(Collectors.toList());
    }

    /**
     * 이번 주 운동 데이터 조회
     * @param userSeq 사용자 시퀀스
     * @return ExerciseTypeResponse 리스트
     */
    public List<ExerciseResponse> getThisWeekExercises(int userSeq) {
        User user = validateUser(userSeq);

        Long weekStart = timeStampUtil.getThisWeekStartTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();

        log.info("이번 주 운동 데이터 조회 - User: {}", userSeq);

        List<Exercise> exercises = exerciseRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, weekStart, now);

        return exercises.stream()
                .map(this::convertToExerciseTypeResponse)
                .collect(Collectors.toList());
    }

    /**
     * 3주치 운동 데이터 조회
     * @param userSeq 사용자 시퀀스
     * @return ExerciseTypeResponse 리스트
     */
    public List<ExerciseResponse> getThreeWeeksExercises(int userSeq) {
        User user = validateUser(userSeq);

        Long threeWeeksAgo = timeStampUtil.getThreeWeeksAgoTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();

        log.info("3주치 운동 데이터 조회 - User: {}", userSeq);

        List<Exercise> exercises = exerciseRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, threeWeeksAgo, now);

        return exercises.stream()
                .map(this::convertToExerciseTypeResponse)
                .collect(Collectors.toList());
    }

    /**
     * 기간별 운동 데이터 조회
     * @param userSeq 사용자 시퀀스
     * @return ExerciseResponse 리스트
     */
    public List<ExerciseResponse> getExercisesByPeriod(
            int userSeq,
            LocalDate startDate,
            LocalDate endDate) {
        User user = validateUser(userSeq);

        Long startDateTs = timeStampUtil.getDateStartTimestamp(startDate);
        Long endDateTs = timeStampUtil.getDateEndTimestamp(endDate);

        log.info("기간별 운동 데이터 조회 - User: {}, startDate: {}, endDate: {}", userSeq, startDate, endDate);

        List<Exercise> exercises = exerciseRepository
                .findByUserAndStartTimeBetweenOrderByStartTimeDesc(user, startDateTs, endDateTs);

        return exercises.stream()
                .map(this::convertToExerciseTypeResponse)
                .collect(Collectors.toList());
    }

    private ExerciseResponse convertToExerciseTypeResponse(Exercise exercise) {
        // ExerciseSession을 ExerciseSessionDto로 변환
        List<ExerciseSessionDto> sessionDtos = exercise.getSessions().stream()
                .map(this::convertToExerciseSessionDto)
                .collect(Collectors.toList());

        ExerciseResponse exerciseResponse = ExerciseResponse.builder()
                .exerciseId(exercise.getExerciseId())
//                .deviceId(exercise.getDeviceId())
//                .deviceType(exercise.getDeviceType())
//                .uid(exercise.getUid())
                .startTime(timeStampUtil.toLocalDateTime(exercise.getStartTime()))
                .endTime(timeStampUtil.toLocalDateTime(exercise.getEndTime()))
                .exerciseType(exercise.getExerciseType())
                .sessions(sessionDtos)
                .build();

        return exerciseResponse;
    }

    /**
     * 기간별 운동 통계 조회
     * @param userSeq 사용자 시퀀스
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 운동 통계
     */
    public ExerciseStatisticsResponse getExerciseStatistics(
            int userSeq,
            Long startTime,
            Long endTime
    ) {
        User user = validateUser(userSeq);

        Long effectiveStartTime = startTime != null ? startTime : timeStampUtil.getThreeWeeksAgoTimestamp();
        Long effectiveEndTime = endTime != null ? endTime : timeStampUtil.getCurrentTimestamp();

        log.info("운동 통계 조회 - User: {}, Start: {}, End: {}", userSeq, effectiveStartTime, effectiveEndTime);

        Float totalCalories = exerciseRepository
                .sumTotalCaloriesByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0.0f);

        Float totalDistance = exerciseRepository
                .sumTotalDistanceByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0.0f);

        Float avgCalories = exerciseRepository
                .avgCaloriesByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0.0f);

        Long exerciseCount = exerciseRepository
                .countByUserAndPeriod(user, effectiveStartTime, effectiveEndTime);

        return ExerciseStatisticsResponse.builder()
                .totalCalories(totalCalories)
                .totalDistance(totalDistance)
                .averageCalories(avgCalories)
                .exerciseCount(exerciseCount)
                .startTime(timeStampUtil.toLocalDateTime(effectiveStartTime))
                .endTime(timeStampUtil.toLocalDateTime(effectiveEndTime))
                .build();
    }

    /**
     * 기간별 운동 통계 조회
     * @param userSeq 사용자 시퀀스
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 운동 통계
     */
    public ExerciseStatisticsResponse getExerciseStatisticsByDate(
            int userSeq,
            LocalDate startTime,
            LocalDate endTime
    ) {
        User user = validateUser(userSeq);

        Long effectiveStartTime = startTime != null ? timeStampUtil.getDateStartTimestamp(startTime) : timeStampUtil.getThreeWeeksAgoTimestamp();
        Long effectiveEndTime = endTime != null ? timeStampUtil.getDateEndTimestamp(endTime) : timeStampUtil.getCurrentTimestamp();

        log.info("(LocalDate) 운동 통계 조회 - User: {}, Start: {}, End: {}", userSeq, effectiveStartTime, effectiveEndTime);

        Float totalCalories = exerciseRepository
                .sumTotalCaloriesByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0.0f);

        Float totalDistance = exerciseRepository
                .sumTotalDistanceByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0.0f);

        Float avgCalories = exerciseRepository
                .avgCaloriesByUserAndPeriod(user, effectiveStartTime, effectiveEndTime)
                .orElse(0.0f);

        Long exerciseCount = exerciseRepository
                .countByUserAndPeriod(user, effectiveStartTime, effectiveEndTime);

        Long totalDuration = exerciseRepository
                .sumTotalDurationByUserAndPeriod(user, effectiveStartTime, effectiveEndTime);

        return ExerciseStatisticsResponse.builder()
                .totalCalories(totalCalories)
                .totalDistance(totalDistance)
                .averageCalories(avgCalories)
                .exerciseCount(exerciseCount)
                .totalDuration(totalDuration)
                .startTime(timeStampUtil.toLocalDateTime(effectiveStartTime))
                .endTime(timeStampUtil.toLocalDateTime(effectiveEndTime))
                .build();
    }

    /**
     * 당일 운동 통계 조회
     */
    public ExerciseStatisticsResponse getTodayExerciseStatistics(int userSeq) {
        Long dayStart = timeStampUtil.getTodayStartTimestamp();
        Long dayEnd = timeStampUtil.getCurrentTimestamp();
        return getExerciseStatistics(userSeq, dayStart, dayEnd);
    }

    /**
     * 이번 주 운동 통계 조회
     */
    public ExerciseStatisticsResponse getThisWeekExerciseStatistics(int userSeq) {
        Long weekStart = timeStampUtil.getThisWeekStartTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();
        return getExerciseStatistics(userSeq, weekStart, now);
    }

    /**
     * 3주치 운동 통계 조회
     */
    public ExerciseStatisticsResponse getThreeWeeksExerciseStatistics(int userSeq) {
        Long threeWeeksAgo = timeStampUtil.getThreeWeeksAgoTimestamp();
        Long now = timeStampUtil.getCurrentTimestamp();
        return getExerciseStatistics(userSeq, threeWeeksAgo, now);
    }

    private ExerciseSessionDto convertToExerciseSessionDto(ExerciseSession session) {
        return ExerciseSessionDto.builder()
                .startTime(timeStampUtil.toLocalDateTime(session.getStartTime()))
                .endTime(timeStampUtil.toLocalDateTime(session.getEndTime()))
                .exerciseType(session.getExerciseType())
                .calories(session.getCalories())
                .distance(session.getDistance())
                .duration(session.getDuration())
                .build();
    }

    private User validateUser(int userSeq) {
        return userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
