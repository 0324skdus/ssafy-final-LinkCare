package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.BloodPressure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BloodPressureRepository extends JpaRepository<BloodPressure, Long> {
}
