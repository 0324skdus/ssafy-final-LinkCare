package com.ssafy.linkcare.group.repository;

import com.ssafy.linkcare.group.entity.Group;
import com.ssafy.linkcare.group.entity.GroupJoinRequest;
import com.ssafy.linkcare.group.enums.RequestStatus;
import com.ssafy.linkcare.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupJoinRequestRepository extends JpaRepository<GroupJoinRequest, Long> {

    // 특정 그룹의 모든 신청 목록 조회
    List<GroupJoinRequest> findByGroup(Group group);

    // 특정 그룹의 신청 목록 조회 (상태별)
    List<GroupJoinRequest> findByGroupAndStatus(Group group, RequestStatus status);

    // 특정 그룹의 대기 중인 신청만 조회 (방장이 볼 목록)
    List<GroupJoinRequest> findByGroup_GroupSeqAndStatus(Long groupSeq, RequestStatus status);

    // 특정 사용자가 특정 그룹에 신청한 이력 조회
    Optional<GroupJoinRequest> findByGroupAndUser(Group group, User user);

    // 중복 신청 방지: 특정 사용자가 특정 그룹에 대기 중인 신청이 있는지 확인
    boolean existsByGroupAndUserAndStatus(Group group, User user, RequestStatus status);

    // 특정 사용자의 모든 신청 이력 조회
    List<GroupJoinRequest> findByUser(User user);

    // 특정 그룹에 특정 사용자가 특정 상태의 신청이 있는지 확인 (userId로)
    boolean existsByGroup_GroupSeqAndUser_UserPkAndStatus(Long groupSeq, Long userPk, RequestStatus status);

    // 특정 사용자의 특정 상태 신청 목록 조회 (신청 목록 탭용)
    List<GroupJoinRequest> findByUser_UserPkAndStatus(Long userPk, RequestStatus status);
}
