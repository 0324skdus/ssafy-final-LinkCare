package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.HeartRate;
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
public interface HeartRateRepository extends JpaRepository<HeartRate, Integer> {

    Optional<HeartRate> findByUserAndUid(User user, String uid);

    @Query("SELECT hr FROM HeartRate hr WHERE hr.user = :user " +
            "AND FUNCTION('DATE', FUNCTION('FROM_UNIXTIME', hr.startTime)) = :startDate")
    List<HeartRate> findByUserAndStartDate(@Param("user") User user,
                                               @Param("startDate") LocalDate startDate);

    @Modifying
    @Query("DELETE FROM HeartRate hr WHERE hr.user = :user AND hr.uid IN :uids")
    void deleteByUserAndUidIn(@Param("user") User user, @Param("uids") Set<String> uids);
}
