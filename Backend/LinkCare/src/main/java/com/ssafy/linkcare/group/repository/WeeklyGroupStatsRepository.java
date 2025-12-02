package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.WeeklyGroupStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyGroupStatsRepository extends JpaRepository<WeeklyGroupStats, Integer> {

    /**
     * 특정 그룹의 최근 N주 통계 조회 (최신순 정렬)
     */
    List<WeeklyGroupStats> findTop3ByGroup_GroupSeqOrderByWeekStartDesc(Long groupSeq);

    /**
     * 특정 그룹의 특정 주차 통계 조회
     */
    Optional<WeeklyGroupStats> findByGroup_GroupSeqAndWeekStart(Long groupSeq, LocalDate weekStart);
}
