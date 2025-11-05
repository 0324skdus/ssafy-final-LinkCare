package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.HeartRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HeartRateRepository extends JpaRepository<HeartRate, Long> {
}
