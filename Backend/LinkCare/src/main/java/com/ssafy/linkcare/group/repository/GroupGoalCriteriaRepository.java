package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupGoalCriteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupGoalCriteriaRepository extends JpaRepository<GroupGoalCriteria, Long> {

    // 그룹으로 목표 기준 조회 (OneToOne 관계)
    Optional<GroupGoalCriteria> findByGroup(Group group);

    // 그룹 ID로 목표 기준 조회
    Optional<GroupGoalCriteria> findByGroup_GroupSeq(Long groupSeq);
}
