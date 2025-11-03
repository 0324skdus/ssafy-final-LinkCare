package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface GroupInvitationRepository extends JpaRepository<GroupInvitation, Long> {

    // 초대 토큰으로 조회 (초대 링크 접속 시 사용)
    Optional<GroupInvitation> findByInvitationToken(String invitationToken);

    // 특정 그룹의 모든 초대 링크 조회
    List<GroupInvitation> findByGroup(Group group);

    // 특정 그룹의 유효한 초대 링크만 조회 (만료되지 않고 사용되지 않은 것)
    List<GroupInvitation> findByGroupAndUsedAtIsNull(Group group);

    // 특정 그룹의 유효한(만료되지 않은) 초대 링크 조회
    @Query("SELECT gi FROM GroupInvitation gi WHERE gi.group = :group AND gi.expiredAt > :now AND gi.usedAt IS NULL")
    Optional<GroupInvitation> findValidInvitationByGroup(@Param("group") Group group, @Param("now") LocalDateTime now);
}
