package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.GroupHealthPermission;
import com.ssafy.linkcare.group.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupHealthPermissionRepository extends JpaRepository<GroupHealthPermission, Long> {

    // 그룹 멤버로 권한 조회 (OneToOne 관계)
    Optional<GroupHealthPermission> findByGroupMember(GroupMember groupMember);

    // 그룹 멤버 ID로 권한 조회
    Optional<GroupHealthPermission> findByGroupMember_GroupMemberSeq(Long groupMemberSeq);
}
