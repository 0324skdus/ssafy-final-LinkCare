package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.ActivitySummary;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivitySummaryRepository extends JpaRepository<ActivitySummary, Integer> {

    @Query("SELECT a FROM ActivitySummary a WHERE a.user = :user " +
            "AND a.startTime = :startTime")
    Optional<ActivitySummary> findByUserAndStartTime(@Param("user") User user,
                                                     @Param("startTime") Long startTime);
}
