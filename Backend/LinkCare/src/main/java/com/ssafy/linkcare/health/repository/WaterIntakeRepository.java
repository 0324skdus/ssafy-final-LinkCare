package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.WaterIntake;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WaterIntakeRepository extends JpaRepository<WaterIntake, Long> {
}
