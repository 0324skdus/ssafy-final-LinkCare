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
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExerciseService {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseSessionRepository exerciseSessionRepository;

    @Lazy
    private final HealthSyncService healthSyncService;

    private final TimeStampUtil timeStampUtil;

    public ExerciseService(
            UserRepository userRepository,
            ExerciseRepository exerciseRepository,
            ExerciseSessionRepository exerciseSessionRepository,
            @Lazy HealthSyncService healthSyncService,
            TimeStampUtil timeStampUtil) {
        this.userRepository = userRepository;
        this.exerciseRepository = exerciseRepository;
        this.exerciseSessionRepository = exerciseSessionRepository;
        this.healthSyncService = healthSyncService;
        this.timeStampUtil = timeStampUtil;
    }

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
                                        .duration(TimeUnit.MILLISECONDS.toSeconds(sessionDto.getDuration()))
                                        .build()
                                ).toList();

                        exerciseSessionRepository.saveAll(sessions);
                    }
                }
            }
        });
    }

    @Transactional
    public void saveWatchExercise(WatchExerciseData data, int userSeq) {
        log.info("워치 운동 데이터 저장 시작: sessionId={}", data.getSessionId());

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자"));

        // Exercise 생성 (uid는 null로 시작)
        Exercise exercise = Exercise.builder()
                .sessionId(data.getSessionId())
                .avgHeartRate(data.getAvgHeartRate())
                .startTime(data.getStartTimestamp())
                .endTime(data.getEndTimestamp())
                .deviceType("WATCH")
                .uid(null)  // 초기에는 null
                .user(user)
                .build();

        // ExerciseSession 생성
        ExerciseSession session = ExerciseSession.builder()
                .exercise(exercise)
                .exerciseType("WALKING")
                .calories(data.getCalories())
                .distance(data.getDistance())
                .duration(data.getDurationSec())
                .startTime(data.getStartTimestamp())
                .endTime(data.getEndTimestamp())
                .build();

        exercise.getSessions().add(session);
        exerciseRepository.save(exercise);

        log.info("워치 운동 데이터 저장 완료: sessionId={}", data.getSessionId());

        // 즉시 삼성헬스 운동 동기화 요청
        healthSyncService.requestExerciseSyncWithWatchMerge(userSeq);
        log.info("삼성헬스 운동 동기화 트리거 완료");
    }

    /**
     * 워치 운동 저장 후 즉시 동기화 (병합 포함)
     */
    @Transactional
    public void saveDailyExerciseDataWithWatchMerge(ExerciseDto exerciseDto, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (exerciseDto.getExercises() == null || exerciseDto.getExercises().isEmpty()) {
            return;
        }

        LocalDate syncDate = exerciseDto.getExercises().get(0).getStartTime().toLocalDate();
        log.info("삼성헬스 동기화 시작 (워치 병합): userId={}, date={}", userSeq, syncDate);

        // 1. 삭제 처리 (MOBILE + 병합된 WATCH 포함)
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

        log.info("삼성헬스 동기화 완료 (워치 병합): 병합={}, 신규={}", mergedCount, newCount);
    }

    /**
     * 매일 자정 정기 동기화 (워치 병합 없음)
     */
    @Transactional
    public void saveDailyExerciseData(ExerciseDto exerciseDto, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (exerciseDto == null || exerciseDto.getExercises() == null || exerciseDto.getExercises().isEmpty()) {
            return;
        }

        LocalDate syncDate = exerciseDto.getExercises().get(0).getStartTime().toLocalDate();
        log.info("삼성헬스 정기 동기화 시작: userId={}, date={}", userSeq, syncDate);

        // 삭제된 uid 처리
        Set<String> sdkUids = exerciseDto.getExercises().stream()
                .map(ExerciseTypeDto::getUid)
                .collect(Collectors.toSet());

        List<Exercise> existingExercises = exerciseRepository.findByUserAndStartDate(user, syncDate);
        Set<String> existingUids = existingExercises.stream()
                .map(Exercise::getUid)
                .filter(uid -> uid != null)
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

        // 각 운동 데이터 처리 (기존 로직)
        for (ExerciseTypeDto dto : exerciseDto.getExercises()) {

            long startTimeTs = dto.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond();
            long endTimeTs = dto.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond();

            Optional<Exercise> existingData = exerciseRepository.findByUserAndUid(user, dto.getUid());

            if (existingData.isPresent()) {
                updateExistingExercise(existingData.get(), dto, user);
            } else {
                createNewExercise(dto, user, startTimeTs, endTimeTs);
            }
        }

        log.info("삼성헬스 정기 동기화 완료: userId={}, date={}", userSeq, syncDate);
    }

    /**
     * 삭제된 운동 처리 (MOBILE + 병합된 WATCH 포함)
     */
    private void handleExerciseDeletion(ExerciseDto exerciseDto, User user, LocalDate syncDate) {
        Set<String> sdkUids = exerciseDto.getExercises().stream()
                .map(ExerciseTypeDto::getUid)
                .collect(Collectors.toSet());

        // MOBILE + uid가 있는 WATCH (병합된 운동) 모두 확인
        List<Exercise> existingExercises = exerciseRepository.findByUserAndStartDate(user, syncDate)
                .stream()
                .filter(e -> "MOBILE".equals(e.getDeviceType()) ||
                        ("WATCH".equals(e.getDeviceType()) && e.getUid() != null))
                .toList();

        Set<String> existingUids = existingExercises.stream()
                .map(Exercise::getUid)
                .filter(uid -> uid != null)
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
     * 1단계: 삼성헬스 운동과 워치 병합 처리
     */
    private boolean processExerciseWithWatchMerge(ExerciseTypeDto samsungExercise, User user) {

        long samsungStart = samsungExercise.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond();
        long samsungEnd = samsungExercise.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond();

        LocalDate exerciseDate = samsungExercise.getStartTime().toLocalDate();

        // 1단계: 당일 모든 워치 운동 중 시간이 겹치는 것 찾기
        List<Exercise> candidateWatchExercises = exerciseRepository
                .findByUserAndStartDate(user, exerciseDate)
                .stream()
                .filter(e -> "WATCH".equals(e.getDeviceType()))
                .filter(e -> e.getUid() == null)  // uid 없는 것만 (아직 병합 안 된 것)
                .filter(e -> isExerciseTimeOverlapping(e.getStartTime(), e.getEndTime(), samsungStart, samsungEnd))
                .toList();

        if (candidateWatchExercises.isEmpty()) {
            log.info("겹치는 워치 운동 없음 → 새 운동 저장");
            saveNewSamsungHealthExercise(samsungExercise, user, samsungStart, samsungEnd);
            return false;
        }

        log.info("겹치는 워치 운동 {}개 발견", candidateWatchExercises.size());

        // 2단계: 매칭 가능한 세션 찾기
        List<MatchedSession> matchedSessions = findMatchingSessions(samsungExercise, candidateWatchExercises);

        if (matchedSessions.isEmpty()) {
            log.info("매칭 가능한 세션 없음 → 새 운동 저장");
            saveNewSamsungHealthExercise(samsungExercise, user, samsungStart, samsungEnd);
            return false;
        }

        // 3단계: 워치 운동에 삼성 세션들 병합 (uid 포함)
        mergeSessionsToWatchExercise(matchedSessions, candidateWatchExercises.get(0), samsungExercise.getUid());

        log.info("✓ 워치 운동과 병합 완료: watchExerciseId={}, uid={}",
                candidateWatchExercises.get(0).getExerciseId(), samsungExercise.getUid());
        return true;
    }

    /**
     * Exercise 시간 겹침 체크
     */
    private boolean isExerciseTimeOverlapping(long start1, long end1, long start2, long end2) {
        // 겹치지 않는 조건: end1 < start2 OR start1 > end2
        return !(end1 < start2 || start1 > end2);
    }

    /**
     * 2단계: 매칭 가능한 세션들 찾기
     */
    private List<MatchedSession> findMatchingSessions(ExerciseTypeDto samsungExercise,
                                                      List<Exercise> watchExercises) {

        List<MatchedSession> matchedSessions = new ArrayList<>();

        for (ExerciseSessionDto samsungSession : samsungExercise.getSessions()) {

            // WALKING 또는 RUNNING만 매칭 대상
            if (!isWalkingOrRunning(samsungSession.getExerciseType())) {
                log.info("매칭 대상 아님: {}", samsungSession.getExerciseType());
                continue;
            }

            long samsungSessionStart = samsungSession.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond();
            long samsungSessionEnd = samsungSession.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond();

            // 워치 운동 시간과 겹치는지 확인
            for (Exercise watchExercise : watchExercises) {
                long watchStart = watchExercise.getStartTime();
                long watchEnd = watchExercise.getEndTime();

                if (isExerciseTimeOverlapping(watchStart, watchEnd, samsungSessionStart, samsungSessionEnd)) {
                    matchedSessions.add(new MatchedSession(samsungSession, watchExercise));
                    log.info("✓ 매칭 세션 발견: {} ({}~{})",
                            samsungSession.getExerciseType(), samsungSessionStart, samsungSessionEnd);
                    break; // 하나의 워치 운동과만 매칭
                }
            }
        }

        return matchedSessions;
    }

    /**
     * WALKING 또는 RUNNING 체크
     */
    private boolean isWalkingOrRunning(String exerciseType) {
        return "WALKING".equals(exerciseType) || "RUNNING".equals(exerciseType);
    }

    /**
     * 3단계: 워치 운동에 삼성 세션들 병합 (uid 포함)
     */
    private void mergeSessionsToWatchExercise(List<MatchedSession> matchedSessions,
                                              Exercise watchExercise,
                                              String samsungUid) {

        // 워치의 기존 세션 (하나만 있음)
        ExerciseSession watchSession = watchExercise.getSessions().get(0);

        // 삼성 세션들의 시작/종료 시간 범위
        long samsungMinStart = matchedSessions.stream()
                .mapToLong(m -> m.samsungSession.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .min()
                .orElse(watchSession.getStartTime());

        long samsungMaxEnd = matchedSessions.stream()
                .mapToLong(m -> m.samsungSession.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                .max()
                .orElse(watchSession.getEndTime());

        // 실제 운동 시간 결정 (더 빠른 시작, 더 빠른 종료)
        long actualStart = Math.min(watchSession.getStartTime(), samsungMinStart);
        long actualEnd = Math.min(watchSession.getEndTime(), samsungMaxEnd);

        log.info("실제 운동 시간: {} ~ {}", actualStart, actualEnd);

        // 삼성 세션들의 칼로리/거리 합산 (비율 조정)
        float totalSamsungCalories = 0f;
        float totalSamsungDistance = 0f;

        for (MatchedSession matched : matchedSessions) {
            ExerciseSessionDto session = matched.samsungSession;
            long sessionStart = session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond();
            long sessionEnd = session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond();

            // 실제 운동 시간과 겹치는 비율 계산
            long overlapStart = Math.max(sessionStart, actualStart);
            long overlapEnd = Math.min(sessionEnd, actualEnd);
            long overlapDuration = overlapEnd - overlapStart;
            long sessionDuration = sessionEnd - sessionStart;

            float ratio = sessionDuration > 0 ? (float) overlapDuration / sessionDuration : 1.0f;

            if (session.getCalories() != null) {
                totalSamsungCalories += session.getCalories() * ratio;
            }
            if (session.getDistance() != null) {
                totalSamsungDistance += session.getDistance() * ratio;
            }

            log.info("세션 비율 조정: {:.1f}%, cal={:.1f}, dist={:.2f}",
                    ratio * 100, session.getCalories() * ratio, session.getDistance() * ratio);
        }

        // 최종 데이터 선택 (30% 기준)
        Float finalCalories = selectBestValue(
                watchSession.getCalories(),
                totalSamsungCalories > 0 ? totalSamsungCalories : null,
                "칼로리"
        );

        Float finalDistance = selectBestValue(
                watchSession.getDistance(),
                totalSamsungDistance > 0 ? totalSamsungDistance : null,
                "거리"
        );

        Long finalDuration = actualEnd - actualStart;

        // 운동 타입 결정 (첫 번째 매칭 세션의 타입)
        String exerciseType = watchSession.getExerciseType();
        if (!matchedSessions.isEmpty()) {
            exerciseType = matchedSessions.get(0).samsungSession.getExerciseType();
        }

        // 기존 세션 삭제
        exerciseSessionRepository.delete(watchSession);

// Exercise 재생성 먼저! (시간 + uid 포함)
        Exercise mergedExercise = Exercise.builder()
                .exerciseId(watchExercise.getExerciseId())  // 기존 ID 유지
                .sessionId(watchExercise.getSessionId())
                .avgHeartRate(watchExercise.getAvgHeartRate())
                .startTime(actualStart)
                .endTime(actualEnd)
                .deviceType(watchExercise.getDeviceType())  // "WATCH" 유지
                .uid(samsungUid)  // 삼성헬스 uid 저장
                .user(watchExercise.getUser())
                .exerciseType(watchExercise.getExerciseType())
                .deviceId(watchExercise.getDeviceId())
                .zoneOffset(watchExercise.getZoneOffset())
                .dataSource(watchExercise.getDataSource())
                .build();

        exerciseRepository.save(mergedExercise);

// 그 다음 세션 생성 (새로 만든 Exercise 참조)
        ExerciseSession mergedSession = ExerciseSession.builder()
                .exercise(mergedExercise)  // 새로 만든 Exercise 참조
                .startTime(actualStart)
                .endTime(actualEnd)
                .exerciseType(exerciseType)
                .calories(finalCalories)
                .distance(finalDistance)
                .duration(finalDuration)
                .build();

        exerciseSessionRepository.save(mergedSession);

        log.info("병합 완료: 시간={}~{}, cal={}, dist={}, uid={}",
                actualStart, actualEnd, finalCalories, finalDistance, samsungUid);
    }

    /**
     * 데이터 선택 로직 (30% 기준)
     */
    private Float selectBestValue(Float watchValue, Float samsungValue, String dataType) {

        // 1. 워치 데이터 없으면 삼성
        if (watchValue == null || watchValue == 0) {
            log.info("{}: 워치 데이터 없음 → 삼성 사용: {}", dataType, samsungValue);
            return samsungValue;
        }

        // 2. 삼성 데이터 없으면 워치
        if (samsungValue == null || samsungValue == 0) {
            log.info("{}: 삼성 데이터 없음 → 워치 사용: {}", dataType, watchValue);
            return watchValue;
        }

        // 3. 둘 다 있으면 차이 비율 계산
        float max = Math.max(watchValue, samsungValue);
        float min = Math.min(watchValue, samsungValue);
        float diffRatio = (max - min) / max;

        // 차이가 30% 이내면 워치 신뢰
        if (diffRatio <= 0.3) {
            log.info("{}: 차이 {:.1f}% → 워치 신뢰: watch={}, samsung={}",
                    dataType, diffRatio * 100, watchValue, samsungValue);
            return watchValue;
        }

        // 차이가 크면 큰 값 신뢰
        log.info("{}: 차이 {:.1f}% → 큰 값 신뢰: watch={}, samsung={}, 선택={}",
                dataType, diffRatio * 100, watchValue, samsungValue, max);
        return max;
    }

    private void saveNewSamsungHealthExercise(ExerciseTypeDto dto,
                                              User user,
                                              long startTimeTs,
                                              long endTimeTs) {

        String uid = dto.getUid();

        log.info("===== UID 조회 시작 =====");
        log.info("조회할 UID: [{}]", uid);
        log.info("조회할 User ID: {}", user.getUserPk());

        Optional<Exercise> existingData = exerciseRepository.findByUserAndUid(user, uid);

        log.info("조회 결과: {}", existingData.isPresent() ? "존재함" : "없음");

        if (existingData.isPresent()) {
            log.info("기존 UID {} 발견 → 업데이트", uid);
            updateExistingExercise(existingData.get(), dto, user);
        } else {
            log.info("새로운 UID {} → 새 운동 데이터 저장", uid);
            createNewExercise(dto, user, startTimeTs, endTimeTs);
        }
    }

    /**
     * 기존 운동 업데이트
     */
    private void updateExistingExercise(Exercise exercise, ExerciseTypeDto dto, User user) {

        // 1. 기존 세션 모두 삭제
        exerciseSessionRepository.deleteByExercise(exercise);

        // 2. Exercise 업데이트 및 저장
        exercise.updateFromDto(dto, user);
        exerciseRepository.save(exercise);

        // 3. 새 세션 생성 및 저장
        if (dto.getSessions() != null && !dto.getSessions().isEmpty()) {
            List<ExerciseSession> sessions = dto.getSessions().stream()
                    .map(session -> ExerciseSession.builder()
                            .startTime(session.getStartTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                            .endTime(session.getEndTime().atZone(ZoneId.systemDefault()).toEpochSecond())
                            .exerciseType(session.getExerciseType())
                            .distance(session.getDistance())
                            .calories(session.getCalories())
                            .duration(TimeUnit.MILLISECONDS.toSeconds(session.getDuration()))
                            .exercise(exercise)
                            .build())
                    .toList();

            exerciseSessionRepository.saveAll(sessions);
            log.info("운동 세션 업데이트 완료 - Exercise UID: {}, Sessions: {}", dto.getUid(), sessions.size());
        }
    }

    /**
     * 매칭된 세션 정보를 담는 내부 클래스
     */
    private static class MatchedSession {
        ExerciseSessionDto samsungSession;
        Exercise watchExercise;

        MatchedSession(ExerciseSessionDto samsungSession, Exercise watchExercise) {
            this.samsungSession = samsungSession;
            this.watchExercise = watchExercise;
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

    public List<ExerciseSessionResponse> getExerciseSessionsByDate(
            int userSeq,
            LocalDate startDate,
            LocalDate endDate
    ) {
        List<ExerciseResponse> exercises = getExercisesByDate(userSeq, startDate, endDate);

        return exercises.stream()
                .flatMap(exercise -> exercise.getSessions().stream() // 각 운동의 세션들을 평탄화
                .map(dto -> ExerciseSessionResponse.builder()
                        .startTime(dto.getStartTime())
                        .endTime(dto.getEndTime())
                        .exerciseType(dto.getExerciseType())
                        .calories(dto.getCalories())
                        .distance(dto.getDistance())
                        .duration(dto.getDuration())
                        .meanPulseRate(exercise.getAvgHeartRate())
                        .build()))
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
                .avgHeartRate(exercise.getAvgHeartRate())
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

        Long effectiveStartTime = timeStampUtil.getDateStartTimestamp(startTime);
        Long effectiveEndTime = timeStampUtil.getDateEndTimestamp(endTime);

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

    private ExerciseSessionResponse convertToExerciseSessionResponse(ExerciseSessionDto session) {
        return ExerciseSessionResponse.builder()
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
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
