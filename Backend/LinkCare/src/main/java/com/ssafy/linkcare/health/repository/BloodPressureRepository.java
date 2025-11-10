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
}
