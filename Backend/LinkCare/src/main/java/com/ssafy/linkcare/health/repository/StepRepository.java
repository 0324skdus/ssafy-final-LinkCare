package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.ActivitySummary;
import com.ssafy.linkcare.health.entity.Step;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StepRepository extends JpaRepository<Step, Integer> {

    @Query("SELECT s FROM Step s WHERE s.user = :user " +
            "AND s.startTime = :startTime")
    Optional<Step> findByUserAndStartTime(@Param("user") User user,
                                          @Param("startTime") Long startTime);
}
