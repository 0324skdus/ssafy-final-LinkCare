package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.ActivitySummary;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivitySummaryRepository extends JpaRepository<ActivitySummary, Integer> {

    @Query("SELECT a FROM ActivitySummary a WHERE a.user = :user " +
            "AND a.startTime = :startTime")
    Optional<ActivitySummary> findByUserAndStartTime(@Param("user") User user,
                                                     @Param("startTime") Long startTime);
    @Query("SELECT as FROM ActivitySummary as WHERE as.user = :user " +
            "AND FUNCTION('DATE', FUNCTION('FROM_UNIXTIME', as.startTime)) = :startDate")
    ActivitySummary findByUserAndStartDate(@Param("user") User user,
                                           @Param("startDate")LocalDate startDate);

    /**
     * 특정 사용자의 기간별 ActivitySummary 조회 (핵심 메서드)
     * @param user 사용자
     * @param startTime 시작 시간 (Unix timestamp)
     * @param endTime 종료 시간 (Unix timestamp)
     * @return ActivitySummary 리스트 (최신순 정렬)
     */
    List<ActivitySummary> findByUserAndStartTimeBetweenOrderByStartTimeDesc(
            User user,
            Long startTime,
            Long endTime
    );

//    List<ActivitySummary> findByUserAndStartTimeBetween(User user, long startTime, long endTime);
    /**
     * 기간별 총 소모 칼로리 조회
     */
    @Query("SELECT SUM(a.totalCaloriesBurned) FROM ActivitySummary a WHERE a.user = :user AND a.startTime BETWEEN :startTime AND :endTime")
    Double findTotalCaloriesByPeriod(@Param("user") User user, @Param("startTime") long startTime, @Param("endTime") long endTime);

    /**
     * 기간별 총 이동 거리 조회
     */
    @Query("SELECT SUM(a.totalDistance) FROM ActivitySummary a WHERE a.user = :user AND a.startTime BETWEEN :startTime AND :endTime")
    Double findTotalDistanceByPeriod(@Param("user") User user, @Param("startTime") long startTime, @Param("endTime") long endTime);

    /**
     * 기간별 평균 소모 칼로리 조회
     */
    @Query("SELECT AVG(a.totalCaloriesBurned) FROM ActivitySummary a WHERE a.user = :user AND a.startTime BETWEEN :startTime AND :endTime")
    Double findAvgCaloriesByPeriod(@Param("user") User user, @Param("startTime") long startTime, @Param("endTime") long endTime);

    /**
     * 기간별 평균 이동 거리 조회
     */
    @Query("SELECT AVG(a.totalDistance) FROM ActivitySummary a WHERE a.user = :user AND a.startTime BETWEEN :startTime AND :endTime")
    Double findAvgDistanceByPeriod(@Param("user") User user, @Param("startTime") long startTime, @Param("endTime") long endTime);
}
