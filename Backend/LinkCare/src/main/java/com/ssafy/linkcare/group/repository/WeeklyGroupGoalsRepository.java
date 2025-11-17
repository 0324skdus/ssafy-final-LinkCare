package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.WeeklyGroupGoals;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface WeeklyGroupGoalsRepository extends JpaRepository<WeeklyGroupGoals, Integer> {

    /**
     * 특정 그룹의 특정 주차 목표 조회
     */
    Optional<WeeklyGroupGoals> findByGroup_GroupSeqAndWeekStart(Long groupSeq, LocalDate weekStart);
}
