package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.WaterIntake;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WaterIntakeRepository extends JpaRepository<WaterIntake, Long> {
    Optional<WaterIntake> findByUserAndUid(User user, String uid);
}
