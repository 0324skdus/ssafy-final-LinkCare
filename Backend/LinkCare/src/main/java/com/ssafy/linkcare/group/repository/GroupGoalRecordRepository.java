package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.GroupGoalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface GroupGoalRecordRepository extends JpaRepository<GroupGoalRecord, Integer> {

    /**
     * 그룹과 주차로 기록 조회 (중복 방지용)
     */
    Optional<GroupGoalRecord> findByGroupSeqAndWeekStart(Long groupSeq, LocalDate weekStart);
}
