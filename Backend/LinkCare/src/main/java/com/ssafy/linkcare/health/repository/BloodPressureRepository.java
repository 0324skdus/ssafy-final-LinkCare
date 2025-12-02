package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.BloodPressure;
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
public interface BloodPressureRepository extends JpaRepository<BloodPressure, Integer> {

    Optional<BloodPressure> findByUserAndUid(User user, String uid);

    @Query("SELECT bp FROM BloodPressure bp WHERE bp.user = :user " +
            "AND FUNCTION('DATE', FUNCTION('FROM_UNIXTIME', bp.startTime)) = :startDate")
    List<BloodPressure> findByUserAndStartDate(@Param("user") User user,
                                               @Param("startDate") LocalDate startDate);

    @Modifying
    @Query("DELETE FROM BloodPressure bp WHERE bp.user = :user AND bp.uid IN :uids")
    void deleteByUserAndUidIn(@Param("user") User user, @Param("uids") Set<String> uids);

    Optional<BloodPressure> findByUserAndStartTime(User user, Long startTimeTs);

    List<BloodPressure> findByUserAndStartTimeBetweenOrderByStartTimeDesc(User user, Long startTimeTs, Long endTimeTs);

    /**
     * 기간별 평균 수축기 혈압 조회
     */
    @Query("SELECT AVG(b.systolic) FROM BloodPressure b WHERE b.user = :user AND b.startTime BETWEEN :startTime AND :endTime")
    Double findAverageSystolicByPeriod(@Param("user") User user, @Param("startTime") long startTime, @Param("endTime") long endTime);

    /**
     * 기간별 평균 이완기 혈압 조회
     */
    @Query("SELECT AVG(b.diastolic) FROM BloodPressure b WHERE b.user = :user AND b.startTime BETWEEN :startTime AND :endTime")
    Double findAverageDiastolicByPeriod(@Param("user") User user, @Param("startTime") long startTime, @Param("endTime") long endTime);

    /**
     * 기간별 평균 맥박 조회
     */
    @Query("SELECT AVG(b.pulseRate) FROM BloodPressure b WHERE b.user = :user AND b.startTime BETWEEN :startTime AND :endTime")
    Double findAveragePulseRateByPeriod(@Param("user") User user, @Param("startTime") long startTime, @Param("endTime") long endTime);

    // 최대/최소 혈압 조회 메서드들
    @Query("SELECT MAX(b.systolic) FROM BloodPressure b " +
            "WHERE b.user = :user AND b.startTime >= :startTimestamp AND b.startTime < :endTimestamp")
    Double findMaxSystolicByPeriod(@Param("user") User user,
                                   @Param("startTimestamp") long startTimestamp,
                                   @Param("endTimestamp") long endTimestamp);

    @Query("SELECT MIN(b.systolic) FROM BloodPressure b " +
            "WHERE b.user = :user AND b.startTime >= :startTimestamp AND b.startTime < :endTimestamp")
    Double findMinSystolicByPeriod(@Param("user") User user,
                                   @Param("startTimestamp") long startTimestamp,
                                   @Param("endTimestamp") long endTimestamp);

    @Query("SELECT MAX(b.diastolic) FROM BloodPressure b " +
            "WHERE b.user = :user AND b.startTime >= :startTimestamp AND b.startTime < :endTimestamp")
    Double findMaxDiastolicByPeriod(@Param("user") User user,
                                    @Param("startTimestamp") long startTimestamp,
                                    @Param("endTimestamp") long endTimestamp);

    @Query("SELECT MIN(b.diastolic) FROM BloodPressure b " +
            "WHERE b.user = :user AND b.startTime >= :startTimestamp AND b.startTime < :endTimestamp")
    Double findMinDiastolicByPeriod(@Param("user") User user,
                                    @Param("startTimestamp") long startTimestamp,
                                    @Param("endTimestamp") long endTimestamp);

    Optional<BloodPressure> findFirstByUserAndStartTimeBetweenOrderByStartTimeDesc(
            User user,
            Long startTime,
            Long endTime);
}
