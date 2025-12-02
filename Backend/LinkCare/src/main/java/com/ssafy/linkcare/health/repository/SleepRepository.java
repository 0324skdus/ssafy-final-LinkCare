package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.Sleep;
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
public interface SleepRepository extends JpaRepository<Sleep, Long> {

    Optional<Sleep> findByUserAndUid(User user, String uid);

    // Unix timestamp(초)를 날짜로 변환해서 비교
    @Query("SELECT s FROM Sleep s WHERE s.user = :user " +
            "AND FUNCTION('DATE', FUNCTION('FROM_UNIXTIME', s.endTime)) = :endDate")
    List<Sleep> findByUserAndEndDate(@Param("user") User user,
                                     @Param("endDate") LocalDate endDate);

    @Modifying
    @Query("DELETE FROM Sleep s WHERE s.user = :user AND s.uid IN :uids")
    void deleteByUserAndUidIn(@Param("user") User user, @Param("uids") Set<String> uids);

    /**
     * 특정 사용자의 기간별 Sleep 조회
     * @param user 사용자
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return Sleep 리스트 (최신순, sessions 포함)
     */
    @Query("SELECT DISTINCT s FROM Sleep s " +
            "LEFT JOIN FETCH s.sessions " +
            "WHERE s.user = :user AND s.endTime BETWEEN :startTime AND :endTime " +
            "ORDER BY s.endTime DESC")
    List<Sleep> findByUserAndEndTimeBetweenOrderByEndTimeDesc(User user, Long startTime, Long endTime);

    /**
     * 특정 사용자의 기간별 총 수면 시간 합계
     */
    @Query("SELECT SUM(s.duration) FROM Sleep s " +
            "WHERE s.user = :user AND s.endTime BETWEEN :startTime AND :endTime")
    Optional<Long> sumTotalDurationByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 평균 수면 시간
     */
    @Query("SELECT AVG(s.duration) FROM Sleep s " +
            "WHERE s.user = :user AND s.endTime BETWEEN :startTime AND :endTime")
    Optional<Double> avgDurationByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 수면 기록 개수
     */
    @Query("SELECT COUNT(s) FROM Sleep s " +
            "WHERE s.user = :user AND s.endTime BETWEEN :startTime AND :endTime")
    Long countByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 최대 수면 시간
     */
    @Query("SELECT MAX(s.duration) FROM Sleep s " +
            "WHERE s.user = :user AND s.endTime BETWEEN :startTime AND :endTime")
    Optional<Integer> maxDurationByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 최소 수면 시간
     */
    @Query("SELECT MIN(s.duration) FROM Sleep s " +
            "WHERE s.user = :user AND s.endTime BETWEEN :startTime AND :endTime")
    Optional<Integer> minDurationByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    List<Sleep> findByUserAndEndTimeBetweenOrderByEndTimeAsc(User user, Long startTime, Long endTime);
}
