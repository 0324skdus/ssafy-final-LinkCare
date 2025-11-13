package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.Sleep;
import com.ssafy.linkcare.health.entity.SleepSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SleepSessionRepository extends JpaRepository<SleepSession, Integer> {

    @Query("SELECT ss FROM SleepSession ss WHERE ss.sleep = :sleep " +
            "AND ss.startTime = :startTime AND ss.endTime = :endTime")
    Optional<SleepSession> findBySleepAndStartTimeAndEndTime(@Param("sleep")Sleep sleep,
                                                                  @Param("startTime") Long startTime,
                                                                  @Param("endTime") Long endTime);


    List<SleepSession> findById(int sleepId);

    List<SleepSession> findBySleep(Sleep sleep);

    void deleteBySleep(Sleep sleep);

    long countBySleep(Sleep sleep);
}
