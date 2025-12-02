package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.BloodPressure;
import com.ssafy.linkcare.health.entity.Exercise;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ExerciseRepository extends JpaRepository<Exercise, Integer> {

    Optional<Exercise> findByUserAndUid(User user, String uid);

    @Query("SELECT e FROM Exercise e WHERE e.user = :user " +
            "AND FUNCTION('DATE', FUNCTION('FROM_UNIXTIME', e.startTime)) = :startDate")
    List<Exercise> findByUserAndStartDate(@Param("user") User user,
                                               @Param("startDate") LocalDate startDate);

    @Modifying
    @Query("DELETE FROM Exercise e WHERE e.user = :user AND e.uid IN :uids")
    void deleteByUserAndUidIn(@Param("user") User user, @Param("uids") Set<String> uids);

    /**
     * 특정 사용자의 기간별 Exercise 조회
     * @param user 사용자
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return Exercise 리스트 (최신순, sessions 포함)
     */
    @Query("SELECT DISTINCT e FROM Exercise e " +
            "LEFT JOIN FETCH e.sessions " +
            "WHERE e.user = :user AND e.startTime BETWEEN :startTime AND :endTime " +
            "ORDER BY e.startTime DESC")
    List<Exercise> findByUserAndStartTimeBetweenOrderByStartTimeDesc(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 총 칼로리 합계
     */
    @Query("SELECT SUM(es.calories) FROM ExerciseSession es " +
            "JOIN es.exercise e " +
            "WHERE e.user = :user AND e.startTime BETWEEN :startTime AND :endTime")
    Optional<Float> sumTotalCaloriesByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 총 거리 합계
     */
    @Query("SELECT SUM(es.distance) FROM ExerciseSession es " +
            "JOIN es.exercise e " +
            "WHERE e.user = :user AND e.startTime BETWEEN :startTime AND :endTime")
    Optional<Float> sumTotalDistanceByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 총 운동 시간 합계
     */
    @Query("SELECT SUM(es.duration) FROM ExerciseSession es " +
            "JOIN es.exercise e " +
            "WHERE e.user = :user AND e.startTime BETWEEN :startTime AND :endTime")
    Long sumTotalDurationByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 운동 횟수
     */
    @Query("SELECT COUNT(e) FROM Exercise e " +
            "WHERE e.user = :user AND e.startTime BETWEEN :startTime AND :endTime")
    Long countByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 평균 칼로리
     */
    @Query("SELECT AVG(es.calories) FROM ExerciseSession es " +
            "JOIN es.exercise e " +
            "WHERE e.user = :user AND e.startTime BETWEEN :startTime AND :endTime")
    Optional<Float> avgCaloriesByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 시간대에 겹치는 운동 찾기
     * (±2분 여유)
     */
    @Query("SELECT e FROM Exercise e " +
            "WHERE e.user = :user " +
            "AND e.startTime <= :endWindow " +
            "AND e.endTime >= :startWindow")
    List<Exercise> findByUserAndStartTimeBetween(
            @Param("user") User user,
            @Param("startWindow") Long startWindow,
            @Param("endWindow") Long endWindow
    );

    /**
     * 워치 데이터만 조회 (sessionId 있는 것)
     */
    @Query("SELECT e FROM Exercise e " +
            "WHERE e.user = :user " +
            "AND e.sessionId IS NOT NULL")
    List<Exercise> findWatchExercisesByUser(@Param("user") User user);
}
