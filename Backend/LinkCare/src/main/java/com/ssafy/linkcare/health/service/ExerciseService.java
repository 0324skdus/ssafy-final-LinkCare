package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.ExerciseDto;
import com.ssafy.linkcare.health.dto.ExerciseSessionDto;
import com.ssafy.linkcare.health.dto.ExerciseTypeDto;
import com.ssafy.linkcare.health.entity.Exercise;
import com.ssafy.linkcare.health.entity.ExerciseSession;
import com.ssafy.linkcare.health.repository.ExerciseRepository;
import com.ssafy.linkcare.health.repository.ExerciseSessionRepository;
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
public class ExerciseService {

    private final UserRepository userRepository;
    private final ExerciseRepository exerciseRepository;
    private final ExerciseSessionRepository exerciseSessionRepository;

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
                                        .exercise(savedExercise)
                                        .build()
                                ).toList();

                        exerciseSessionRepository.saveAll(sessions);
                    }
                }

            }
        }
    }
}
