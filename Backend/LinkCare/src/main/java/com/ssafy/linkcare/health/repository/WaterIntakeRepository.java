package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.WaterIntake;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaterIntakeRepository extends JpaRepository<WaterIntake, Long> {
    Optional<WaterIntake> findByUserAndUid(User user, String uid);

    /**
     * 특정 사용자의 기간별 WaterIntake 조회
     * @param user 사용자
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return WaterIntake 리스트 (최신순)
     */
    List<WaterIntake> findByUserAndStartTimeBetweenOrderByStartTimeDesc(
            User user,
            Long startTime,
            Long endTime
    );

    /**
     * 특정 사용자의 기간별 총 물 섭취량 합계
     */
    @Query("SELECT SUM(w.amount) FROM WaterIntake w " +
            "WHERE w.user = :user AND w.startTime BETWEEN :startTime AND :endTime")
    Optional<Float> sumTotalAmountByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 평균 물 섭취량
     */
    @Query("SELECT AVG(w.amount) FROM WaterIntake w " +
            "WHERE w.user = :user AND w.startTime BETWEEN :startTime AND :endTime")
    Optional<Float> avgAmountByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 물 섭취 기록 개수
     */
    @Query("SELECT COUNT(w) FROM WaterIntake w " +
            "WHERE w.user = :user AND w.startTime BETWEEN :startTime AND :endTime")
    Long countByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 최신 물 섭취 목표 조회
     */
    @Query("SELECT w.goal FROM WaterIntake w " +
            "WHERE w.user = :user " +
            "ORDER BY w.startTime DESC " +
            "LIMIT 1")
    Optional<Float> findLatestGoalByUser(@Param("user") User user);

    @Query("SELECT w FROM WaterIntake w WHERE w.user = :user " +
            "AND FUNCTION('DATE', FUNCTION('FROM_UNIXTIME', w.startTime)) = :startDate")
    List<WaterIntake> findByUserAndStartDate(User user, LocalDate today);
}
