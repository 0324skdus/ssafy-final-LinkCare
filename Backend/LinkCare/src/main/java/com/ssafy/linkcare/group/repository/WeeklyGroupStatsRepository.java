package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.WeeklyGroupStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyGroupStatsRepository extends JpaRepository<WeeklyGroupStats, Integer> {

}
