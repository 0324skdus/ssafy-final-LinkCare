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
}
