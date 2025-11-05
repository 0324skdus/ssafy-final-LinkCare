package com.ssafy.linkcare.health.repository;

import com.ssafy.linkcare.health.entity.ExerciseSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExerciseSessionRepository extends JpaRepository<ExerciseSession, Long> {
}
