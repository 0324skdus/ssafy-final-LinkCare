package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupMember;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    // 특정 그룹의 모든 멤버 조회
    List<GroupMember> findByGroup(Group group);

    // 특정 그룹의 멤버 수 카운트
    int countByGroup(Group group);

    // 특정 사용자가 속한 모든 그룹 조회
    List<GroupMember> findByUser(User user);

    // 특정 사용자가 특정 그룹의 멤버인지 확인
    boolean existsByGroupAndUser(Group group, User user);

    // 특정 그룹의 방장 조회
    Optional<GroupMember> findByGroupAndIsLeaderTrue(Group group);

    // 특정 사용자가 특정 그룹의 방장인지 확인
    boolean existsByGroupAndUserAndIsLeaderTrue(Group group, User user);

    // N+1 문제 해결: 그룹 정보와 함께 조회
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.group " +
            "WHERE gm.user.userPk = :userId")
    List<GroupMember> findByUserWithGroup(@Param("userId") Long userId);

    // 특정 그룹에서 특정 사용자의 멤버 정보 조회
    Optional<GroupMember> findByGroupAndUser_UserPk(Group group, Long userPk);

    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user " +
            "WHERE gm.group = :group")
    List<GroupMember> findByGroupWithUser(@Param("group") Group group);

    // 특정 그룹에 특정 사용자가 멤버인지 확인 (userId로)
    boolean existsByGroupAndUser_UserPk(Group group, Long userPk);

    // 그룹 상세 조회 시 N+1 문제 해결을 위한 쿼리
    @Query("SELECT gm FROM GroupMember gm " +
            "JOIN FETCH gm.user u " +
            "LEFT JOIN FETCH u.mainCharacter mc " +
            "LEFT JOIN FETCH u.mainBackground mb " +
            "WHERE gm.group = :group")
    List<GroupMember> findMembersWithDetailsByGroup(@Param("group") Group group);

    Optional<GroupMember> findByUser_UserPkAndGroup_GroupSeq(Long userPk, Long groupSeq);
}
