package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.Exercise;
import com.ssafy.linkcare.health.entity.ExerciseSession;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ExerciseSessionRepository extends JpaRepository<ExerciseSession, Integer> {

    @Query("SELECT es FROM ExerciseSession es WHERE es.exercise = :exercise " +
            "AND es.startTime = :startTime AND es.endTime = :endTime")
    Optional<ExerciseSession> findByExerciseAndStartTimeAndEndTime(
            @Param("exercise") Exercise exercise,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    List<ExerciseSession> findByExercise(Exercise exercise);

    void deleteByExercise(Exercise exercise);

    long countByExercise(Exercise exercise);

    /**
     * 특정 운동의 특정 시간대 세션 찾기
     * (±2분 여유)
     */
    @Query("SELECT es FROM ExerciseSession es " +
            "WHERE es.exercise = :exercise " +
            "AND es.startTime <= :endWindow " +
            "AND es.endTime >= :startWindow")
    Optional<ExerciseSession> findByExerciseAndTimeBetween(
            @Param("exercise") Exercise exercise,
            @Param("startWindow") Long startWindow,
            @Param("endWindow") Long endWindow
    );
}
