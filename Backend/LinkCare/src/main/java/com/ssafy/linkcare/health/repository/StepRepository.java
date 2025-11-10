package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.ActivitySummary;
import com.ssafy.linkcare.health.entity.Step;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StepRepository extends JpaRepository<Step, Integer> {

    @Query("SELECT s FROM Step s WHERE s.user = :user " +
            "AND s.startTime = :startTime")
    Optional<Step> findByUserAndStartTime(@Param("user") User user,
                                          @Param("startTime") Long startTime);

    List<Step> findByUserAndStartTimeBetweenOrderByStartTimeDesc(User user, Long startTimeTs, Long endTimeTs);

    @Query("SELECT s FROM Step s WHERE s.user = :user " +
            "AND FUNCTION('DATE', FUNCTION('FROM_UNIXTIME', s.startTime)) = :startDate")
    Step findByUserAndStartDate(@Param("user") User user,
                                           @Param("startDate") LocalDate startDate);

    /**
     * 특정 사용자의 기간별 총 걸음 수
     */
    @Query("SELECT SUM(s.count) FROM Step s " +
            "WHERE s.user = :user AND s.startTime BETWEEN :startTime AND :endTime")
    Optional<Long> sumTotalCountByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );

    /**
     * 특정 사용자의 기간별 평균 걸음 수
     */
    @Query("SELECT AVG(s.count) FROM Step s " +
            "WHERE s.user = :user AND s.startTime BETWEEN :startTime AND :endTime")
    Optional<Float> avgCountByUserAndPeriod(
            @Param("user") User user,
            @Param("startTime") Long startTime,
            @Param("endTime") Long endTime
    );
}
