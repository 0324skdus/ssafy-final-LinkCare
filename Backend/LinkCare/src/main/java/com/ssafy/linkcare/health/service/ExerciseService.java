package com.ssafy.linkcare.health.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.health.dto.ExerciseDto;
import com.ssafy.linkcare.health.dto.ExerciseTypeDto;
import com.ssafy.linkcare.health.entity.Exercise;
import com.ssafy.linkcare.health.entity.ExerciseSession;
import com.ssafy.linkcare.health.repository.ExerciseRepository;
import com.ssafy.linkcare.health.repository.ExerciseSessionRepository;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

        exerciseList.forEach(exerciseDto -> {

            if(exerciseDto.getExercises() == null || exerciseDto.getExercises().isEmpty()) {
                return;
            }

            exerciseDto.getExercises().forEach(dto -> {

                Exercise saveExercise = exerciseRepository.save(
                        Exercise.builder()
                                .deviceId(dto.getDeviceId())
                                .deviceType(dto.getDeviceType())
                                .uid(dto.getUid())
                                .zoneOffset(dto.getZoneOffset())
                                .startTime(dto.getStartTime())
                                .endTime(dto.getEndTime())
                                .dataSource(dto.getDataSource())
                                .exerciseType(dto.getExerciseType())
                                .user(user)
                                .build()
                );

                if(dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                    List<ExerciseSession> sessions = dto.getSessions().stream()
                            .map(sessionDto -> ExerciseSession.builder()
                                    .exercise(saveExercise)
                                    .startTime(sessionDto.getStartTime())
                                    .endTime(sessionDto.getEndTime())
                                    .exerciseType(sessionDto.getExerciseType())
                                    .calories(sessionDto.getCalories())
                                    .distance(sessionDto.getDistance())
                                    .build()
                            ).toList();

                    exerciseSessionRepository.saveAll(sessions);
                }
            });
        });
    }

    public void saveDailyExerciseData(ExerciseDto exerciseDto, int userSeq) {

        User user = userRepository.findById((long) userSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if(exerciseDto.getExercises() == null || exerciseDto.getExercises().isEmpty()) {
            return;
        }

        exerciseDto.getExercises().forEach(dto -> {
            Exercise savedExercise = Exercise.builder()
                    .deviceId(dto.getDeviceId())
                    .deviceType(dto.getDeviceType())
                    .uid(dto.getUid())
                    .exerciseType(dto.getExerciseType())
                    .zoneOffset(dto.getZoneOffset())
                    .dataSource(dto.getDataSource())
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .user(user)
                    .build();

            exerciseRepository.save(savedExercise);

            if(dto.getSessions() != null && !dto.getSessions().isEmpty()) {
                List<ExerciseSession> sessions = dto.getSessions().stream()
                        .map(session -> ExerciseSession.builder()
                                .startTime(session.getStartTime())
                                .endTime(session.getEndTime())
                                .exerciseType(session.getExerciseType())
                                .distance(session.getDistance())
                                .calories(session.getCalories())
                                .exercise(savedExercise)
                                .build()
                        ).toList();

                exerciseSessionRepository.saveAll(sessions);
            }
        });
    }
}
