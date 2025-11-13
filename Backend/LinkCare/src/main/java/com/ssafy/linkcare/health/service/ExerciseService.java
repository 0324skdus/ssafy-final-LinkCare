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

        LocalDate syncDate = exerciseDto.getExercises().get(0).getStartTime().toLocalDate();

        log.info("삼성헬스 동기화 시작: userId={}, date={}", userSeq, syncDate);

        // 1. 삭제 처리
        handleExerciseDeletion(exerciseDto, user, syncDate);

        // 2. 각 운동 처리 (워치 병합 포함!)
        int mergedCount = 0;
        int newCount = 0;

        for (ExerciseTypeDto dto : exerciseDto.getExercises()) {
            boolean merged = processExerciseWithWatchMerge(dto, user);
            if (merged) {
                mergedCount++;
            } else {
                newCount++;
            }
        }

        log.info("삼성헬스 동기화 완료: 병합={}, 신규={}", mergedCount, newCount);
    }

    /**
     * 삭제된 운동 처리
     */
    private void handleExerciseDeletion(ExerciseDto exerciseDto, User user, LocalDate syncDate) {
        Set<String> sdkUids = exerciseDto.getExercises().stream()
                .map(ExerciseTypeDto::getUid)
                .collect(Collectors.toSet());

        List<Exercise> existingExercises = exerciseRepository.findByUserAndStartDate(user, syncDate)
                .stream()
                .filter(e -> "MOBILE".equals(e.getDeviceType()))  // 추가!
                .toList();

        Set<String> existingUids = existingExercises.stream()
                .map(Exercise::getUid)
                .collect(Collectors.toSet());

        Set<String> uidToDelete = existingUids.stream()
                .filter(uid -> !sdkUids.contains(uid))
                .collect(Collectors.toSet());

        if (!uidToDelete.isEmpty()) {
            List<Exercise> exerciseToDelete = existingExercises.stream()
                    .filter(exercise -> uidToDelete.contains(exercise.getUid()))
                    .toList();
            exerciseRepository.deleteAll(exerciseToDelete);
            log.info("삭제된 운동: {}개", exerciseToDelete.size());
        }
    }

    /**
     * 운동 처리 (워치 병합 포함!)
     */
    private boolean processExerciseWithWatchMerge(ExerciseTypeDto dto, User user) {

        long startTimeTs = dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond();
        long endTimeTs = dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond();

        // 1. 같은 시간대 워치 데이터 찾기 (±2분)
        long startWindow = startTimeTs - 120;
        long endWindow = endTimeTs + 120;

        List<Exercise> watchExercises = exerciseRepository
                .findByUserAndStartTimeBetween(user, startWindow, endWindow)
                .stream()
                .filter(e -> "WATCH".equals(e.getDeviceType()))
                .toList();

        boolean merged = false;

        // 2. 워치 데이터 있으면 세션별 병합
        if (!watchExercises.isEmpty()) {
            log.info("워치 데이터 발견! 병합 시작");

            for (ExerciseSessionDto sessionDto : dto.getSessions()) {
                merged = mergeSessionWithWatch(sessionDto, watchExercises, user) || merged;
            }
        }

        // 3. 병합 안 된 세션은 새로 저장
        if (!merged) {
            saveNewSamsungHealthExercise(dto, user, startTimeTs, endTimeTs);
        }

        return merged;
    }

    /**
     * 세션을 워치 데이터와 병합
     */
    private boolean mergeSessionWithWatch(ExerciseSessionDto sessionDto,
                                          List<Exercise> watchExercises,
                                          User user) {

        long sessionStartTs = sessionDto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond();
        long sessionEndTs = sessionDto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond();

        for (Exercise watchExercise : watchExercises) {
            for (ExerciseSession watchSession : watchExercise.getSessions()) {

                if (isTimeOverlapping(watchSession, sessionStartTs, sessionEndTs)) {
                    log.info("워치 세션과 병합: sessionId={}", watchExercise.getSessionId());

                    // 새 세션 생성 (빈 곳 채워서)
                    ExerciseSession updatedSession = createMergedSession(watchSession, sessionDto, watchExercise);

                    // 기존 세션 삭제하고 새 세션 저장
                    exerciseSessionRepository.delete(watchSession);
                    exerciseSessionRepository.save(updatedSession);

                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 시간 겹침 체크
     */
    private boolean isTimeOverlapping(ExerciseSession watchSession,
                                      long samsungStart,
                                      long samsungEnd) {
        long watchStart = watchSession.getStartTime();
        long watchEnd = watchSession.getEndTime();

        return !(samsungEnd < watchStart - 120 || samsungStart > watchEnd + 120);
    }

    /**
     * 병합된 새 세션 생성 (워치 우선 + 빈 곳 채움)
     */
    private ExerciseSession createMergedSession(ExerciseSession watchSession,
                                                ExerciseSessionDto samsungSession,
                                                Exercise exercise) {

        // 워치 데이터 우선, 없으면 삼성헬스 데이터
        Float calories = watchSession.getCalories() != null ?
                watchSession.getCalories() : samsungSession.getCalories();

        Float distance = watchSession.getDistance() != null ?
                watchSession.getDistance() : samsungSession.getDistance();

        Long duration = watchSession.getDuration() != null ?
                watchSession.getDuration() : samsungSession.getDuration();

        return ExerciseSession.builder()
                .exercise(exercise)
                .startTime(watchSession.getStartTime())
                .endTime(watchSession.getEndTime())
                .exerciseType(watchSession.getExerciseType())
                .calories(calories)
                .distance(distance)
                .duration(duration)
                .build();
    }

    /**
     * 새 삼성헬스 운동 저장
     */
    private void saveNewSamsungHealthExercise(ExerciseTypeDto dto,
                                              User user,
                                              long startTimeTs,
                                              long endTimeTs) {

        Optional<Exercise> existingData = exerciseRepository.findByUserAndUid(user, dto.getUid());

        if (existingData.isPresent()) {
            updateExistingExercise(existingData.get(), dto, user);
        } else {
            createNewExercise(dto, user, startTimeTs, endTimeTs);
        }
    }

    /**
     * 기존 운동 업데이트
     */
    private void updateExistingExercise(Exercise exercise, ExerciseTypeDto dto, User user) {

        // 세션 삭제 처리
        if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
            Set<String> sdkSessionKeys = dto.getSessions().stream()
                    .map(session -> session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond()
                            + "_" + session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                    .collect(Collectors.toSet());

            List<ExerciseSession> existingSessions = exerciseSessionRepository.findByExercise(exercise);
            Set<String> existingSessionKeys = existingSessions.stream()
                    .map(session -> session.getStartTime() + "_" + session.getEndTime())
                    .collect(Collectors.toSet());

            Set<String> sessionToDelete = existingSessionKeys.stream()
                    .filter(key -> !sdkSessionKeys.contains(key))
                    .collect(Collectors.toSet());

            if (!sessionToDelete.isEmpty()) {
                existingSessions.stream()
                        .filter(session -> sessionToDelete.contains(session.getStartTime() + "_" + session.getEndTime()))
                        .forEach(exerciseSessionRepository::delete);
            }
        } else {
            exerciseSessionRepository.deleteByExercise(exercise);
        }

        long remainingSessionCount = exerciseSessionRepository.countByExercise(exercise);

        if (remainingSessionCount == 0) {
            exerciseRepository.delete(exercise);
        } else {
            // Exercise 재생성
            Exercise updatedExercise = Exercise.builder()
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

            exerciseRepository.save(updatedExercise);

            // 세션 재생성
            if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                List<ExerciseSession> sessions = dto.getSessions().stream()
                        .map(session -> ExerciseSession.builder()
                                .startTime(session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                .endTime(session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                                .exerciseType(session.getExerciseType())
                                .distance(session.getDistance())
                                .calories(session.getCalories())
                                .duration(session.getDuration())
                                .exercise(updatedExercise)
                                .build())
                        .toList();

                exerciseSessionRepository.saveAll(sessions);
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
    public List<ExerciseResponse> getExercisesByDate(
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

        Long totalDuration = exerciseRepository
                .sumTotalDurationByUserAndPeriod(user, effectiveStartTime, effectiveEndTime);

        return ExerciseStatisticsResponse.builder()
                .totalCalories(totalCalories)
                .totalDistance(totalDistance)
                .totalDuration(totalDuration)
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

    /**
     * 워치 운동 데이터 저장 (즉시)
     */
    public void saveWatchExercise(WatchExerciseData data, int userSeq) {
        log.info("워치 운동 데이터 저장 시작: sessionId={}", data.getSessionId());

        // 사용자 검증
        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));

        // Exercise 생성 (메타데이터)
        Exercise exercise = Exercise.builder()
                .sessionId(data.getSessionId())
                .avgHeartRate(data.getAvgHeartRate())
                .startTime(data.getStartTimestamp())
                .endTime(data.getEndTimestamp())
                .deviceType("WATCH")
                .user(user)
                .build();

        // ExerciseSession 생성 (실제 데이터)
        ExerciseSession session = ExerciseSession.builder()
                .exercise(exercise)
                .calories(data.getCalories())
                .distance(data.getDistance())
                .duration(data.getDurationSec())
                .startTime(data.getStartTimestamp())
                .endTime(data.getEndTimestamp())
                .build();

        // Exercise에 Session 추가
        exercise.getSessions().add(session);

        // 저장
        exerciseRepository.save(exercise);

        log.info("워치 운동 데이터 저장 완료: sessionId={}", data.getSessionId());
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

    /**
     * 새 운동 생성
     */
    private void createNewExercise(ExerciseTypeDto dto, User user, long startTimeTs, long endTimeTs) {

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
        log.info("새 운동 데이터 저장 - User: {}, UID: {}", user.getUserPk(), dto.getUid());

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
                            .build())
                    .toList();

            exerciseSessionRepository.saveAll(sessions);
            log.info("운동 세션 저장 - Exercise UID: {}, Sessions: {}", dto.getUid(), sessions.size());
        }
    }
//    @Transactional
//    public void saveDailyExerciseData(ExerciseDto exerciseDto, int userSeq) {
//
//        User user = userRepository.findById((long) userSeq)
//                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
//
//        if (exerciseDto.getExercises() == null || exerciseDto.getExercises().isEmpty()) {
//            return;
//        }
//        // 삭제만 가능
//        Set<String> sdkUids = exerciseDto.getExercises().stream().map(ExerciseTypeDto::getUid).collect(Collectors.toSet());
//
//        // 동기화 날짜
//        LocalDate syncDate = exerciseDto.getExercises().get(0).getStartTime().toLocalDate();
//
//        // db 에 있는 uid
//        List<Exercise> existingExercises = exerciseRepository.findByUserAndStartDate(user, syncDate);
//        Set<String> existingUids = existingExercises.stream().map(Exercise::getUid).collect(Collectors.toSet());
//
//        // 삭제된 uid 리스트들
//        Set<String> uidToDelete = existingUids.stream().filter(uid -> !sdkUids.contains(uid)).collect(Collectors.toSet());
//
//        // 삭제
//        if (!uidToDelete.isEmpty()) {
//            List<Exercise> exerciseToDelete = existingExercises.stream()
//                    .filter(exercise -> uidToDelete.contains(exercise.getUid()))
//                    .toList();
//            exerciseRepository.deleteAll(exerciseToDelete);
//        }
//
//        for (ExerciseTypeDto dto : exerciseDto.getExercises()) {
//
//            long startTimeTs = dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond();
//            long endTimeTs = dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond();
//
//            Optional<Exercise> existingData = exerciseRepository.findByUserAndUid(user, dto.getUid());
//            if (existingData.isPresent()) {
//                Exercise exercise = existingData.get();
//                // 삭제된 세션이 있는지 확인
//                if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
//                    Set<String> sdkSessionKeys = dto.getSessions().stream().map(session ->
//                            session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond() + "_"
//                                    + session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond()
//                    ).collect(Collectors.toSet());
//
//                    List<ExerciseSession> existingSessions = exerciseSessionRepository.findByExercise(exercise);
//                    Set<String> existingSessionKeys = existingSessions.stream().map(
//                            session -> session.getStartTime() + "_"
//                                    + session.getEndTime()).collect(Collectors.toSet());
//
//                    // 삭제된 session 확인
//                    Set<String> sessionToDelete = existingSessionKeys.stream().filter(key -> !sdkSessionKeys.contains(key))
//                            .collect(Collectors.toSet());
//
//                    // 삭제
//                    if (!sessionToDelete.isEmpty()) {
//                        existingSessions.stream()
//                                .filter(session -> sessionToDelete.contains(
//                                        session.getStartTime() + "_" + session.getEndTime()))
//                                .forEach(exerciseSessionRepository::delete);
//                    }
//                } else {
//                    // 세션이 비어있다면
//                    exerciseSessionRepository.deleteByExercise(exercise);
//                }
//
//                long remainingSessionCount = exerciseSessionRepository.countByExercise(exercise);
//                if (remainingSessionCount == 0) {
//                    // 세션이 하나도 없으면 Exercise도 삭제
//                    exerciseRepository.delete(exercise);
//
//                } else {
//                    Exercise savedExercise = Exercise.builder()
//                            .deviceId(dto.getDeviceId())
//                            .deviceType(dto.getDeviceType())
//                            .uid(dto.getUid())
//                            .exerciseType(dto.getExerciseType())
//                            .zoneOffset(dto.getZoneOffset())
//                            .dataSource(dto.getDataSource())
//                            .startTime(dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
//                            .endTime(dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
//                            .user(user)
//                            .build();
//
//                    exerciseRepository.save(savedExercise);
//
//                    if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
//                        List<ExerciseSession> sessions = dto.getSessions().stream()
//                                .map(session -> ExerciseSession.builder()
//                                        .startTime(session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
//                                        .endTime(session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
//                                        .exerciseType(session.getExerciseType())
//                                        .distance(session.getDistance())
//                                        .calories(session.getCalories())
//                                        .duration(session.getDuration())
//                                        .exercise(savedExercise)
//                                        .build()
//                                ).toList();
//
//                        exerciseSessionRepository.saveAll(sessions);
//                    }
//                }
//
//            } else {
//                Exercise newExercise = Exercise.builder()
//                        .deviceId(dto.getDeviceId())
//                        .deviceType(dto.getDeviceType())
//                        .uid(dto.getUid())
//                        .exerciseType(dto.getExerciseType())
//                        .zoneOffset(dto.getZoneOffset())
//                        .dataSource(dto.getDataSource())
//                        .startTime(startTimeTs)
//                        .endTime(endTimeTs)
//                        .user(user)
//                        .build();
//
//                Exercise savedExercise = exerciseRepository.save(newExercise);
//                log.info("새 운동 데이터 저장 - User: {}, UID: {}", userSeq, dto.getUid());
//
//                // 세션 저장
//                if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
//                    List<ExerciseSession> sessions = dto.getSessions().stream()
//                            .map(session -> ExerciseSession.builder()
//                                    .startTime(session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
//                                    .endTime(session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
//                                    .exerciseType(session.getExerciseType())
//                                    .distance(session.getDistance())
//                                    .calories(session.getCalories())
//                                    .duration(session.getDuration())
//                                    .exercise(savedExercise)
//                                    .build()
//                            ).toList();
//
//                    exerciseSessionRepository.saveAll(sessions);
//                    log.info("운동 세션 저장 - Exercise UID: {}, Sessions: {}", dto.getUid(), sessions.size());
//                }
//            }
//        }
//
//    }
}
