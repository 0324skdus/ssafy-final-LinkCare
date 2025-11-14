package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.WeeklyGroupGoals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyGroupGoalsRepository extends JpaRepository<WeeklyGroupGoals, Integer> {
    // 기본 CRUD 메서드만 사용
}
