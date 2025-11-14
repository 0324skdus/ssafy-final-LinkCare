package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.enums.GroupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    // 그룹 타입으로 조회
    List<Group> findByType(GroupType type);

    // 그룹 이름으로 검색 (LIKE 검색)
    List<Group> findByGroupNameContaining(String keyword);

    // 그룹 이름과 타입으로 검색
    List<Group> findByGroupNameContainingAndType(String keyword, GroupType type);

    // 모든 그룹 ID만 조회 (스케줄러용 - 가벼운 쿼리)
    @Query("SELECT g.groupSeq FROM Group g")
    List<Long> findAllGroupSeqs();
}
