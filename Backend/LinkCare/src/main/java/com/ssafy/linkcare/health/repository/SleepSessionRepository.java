package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.SleepSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SleepSessionRepository extends JpaRepository<SleepSession, Long> {
}
