package com.ssafy.linkcare.group.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.group.dto.*;
import com.ssafy.linkcare.group.entity.*;
import com.ssafy.linkcare.group.enums.GroupType;
import com.ssafy.linkcare.group.repository.*;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupGoalCriteriaRepository goalCriteriaRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupHealthPermissionRepository healthPermissionRepository;
    private final UserRepository userRepository;

    /*
        * 헬스 그룹 생성
            * - 그룹 생성
            * - 목표 기준 설정 (선택)
            * - 생성자를 방장으로 등록
            * - 필수 권한 설정 (걸음수, 심박수, 운동)
    */
    @Transactional
    public GroupResponse createHealthGroup(Long userId, CreateHealthGroupRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 그룹 생성
        Group group = Group.builder()
                .groupName(request.groupName())
                .groupDescription(request.groupDescription())
                .type(GroupType.HEALTH)
                .capacity(request.capacity())
                .build();
        groupRepository.save(group);

        // 3. 목표 기준 생성 (있는 경우만)
        if (request.goalCriteria() != null) {
            GoalCriteriaDto criteria = request.goalCriteria();
            GroupGoalCriteria goalCriteria = GroupGoalCriteria.builder()
                    .group(group)
                    .minCalorie(criteria.minCalorie())
                    .minStep(criteria.minStep())
                    .minDistance(criteria.minDistance())
                    .minDuration(criteria.minDuration())
                    .build();
            goalCriteriaRepository.save(goalCriteria);
        }

        // 4. 생성자를 방장으로 등록
        GroupMember leader = GroupMember.builder()
                .group(group)
                .user(user)
                .isLeader(true)
                .build();
        groupMemberRepository.save(leader);

        // 5. 필수 권한 설정 (헬스 그룹: 걸음수, 심박수, 운동 필수)
        GroupHealthPermission permission = GroupHealthPermission.builder()
                .groupMember(leader)
                .isDailyStepAllowed(true)
                .isHeartRateAllowed(true)
                .isExerciseAllowed(true)
                .isSleepAllowed(false)
                .isWaterIntakeAllowed(false)
                .isBloodPressureAllowed(false)
                .isBloodSugarAllowed(false)
                .build();
        healthPermissionRepository.save(permission);

        log.info("헬스 그룹 생성 완료: groupSeq={}, userId={}", group.getGroupSeq(), userId);

        // 6. 응답 생성
        return new GroupResponse(
                group.getGroupSeq(),
                group.getGroupName(),
                group.getGroupDescription(),
                group.getType(),
                group.getCapacity(),
                1,  // 현재 멤버 수 (방장 1명)
                group.getCreatedAt()
        );
    }

    /*
        * 케어 그룹 생성
            * - 그룹 생성 (목표 기준 없음)
            * - 생성자를 방장으로 등록
            * - 권한 설정 (필수 + 선택 동의 항목)
    */
    @Transactional
    public GroupResponse createCareGroup(Long userId, CreateCareGroupRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 그룹 생성
        Group group = Group.builder()
                .groupName(request.groupName())
                .groupDescription(request.groupDescription())
                .type(GroupType.CARE)
                .capacity(request.capacity())
                .build();
        groupRepository.save(group);

        // 3. 생성자를 방장으로 등록
        GroupMember leader = GroupMember.builder()
                .group(group)
                .user(user)
                .isLeader(true)
                .build();
        groupMemberRepository.save(leader);

        // 4. 권한 설정 (필수 + 선택 항목)
        HealthPermissionDto permissions = request.permissions() != null
                ? request.permissions()
                : new HealthPermissionDto();  // 기본값 (선택 항목 모두 false)

        GroupHealthPermission permission = GroupHealthPermission.builder()
                .groupMember(leader)
                // 필수 항목 (항상 true)
                .isDailyStepAllowed(true)
                .isHeartRateAllowed(true)
                .isExerciseAllowed(true)
                // 선택 항목 (요청값 사용)
                .isSleepAllowed(permissions.isSleepAllowed())
                .isWaterIntakeAllowed(permissions.isWaterIntakeAllowed())
                .isBloodPressureAllowed(permissions.isBloodPressureAllowed())
                .isBloodSugarAllowed(permissions.isBloodSugarAllowed())
                .build();
        healthPermissionRepository.save(permission);

        log.info("케어 그룹 생성 완료: groupSeq={}, userId={}", group.getGroupSeq(), userId);

        // 5. 응답 생성
        return new GroupResponse(
                group.getGroupSeq(),
                group.getGroupName(),
                group.getGroupDescription(),
                group.getType(),
                group.getCapacity(),
                1,  // 현재 멤버 수 (방장 1명)
                group.getCreatedAt()
        );
    }

    // 그룹 상세 조회 (멤버, 목표 기준, 권한 포함)
    public GroupDetailResponse getGroupDetail(Long groupSeq) {
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 그룹 멤버 조회 (N+1 방지 - 추후 최적화 가능)
        List<GroupMember> members = groupMemberRepository.findByGroup(group);

        // 멤버 DTO 변환 (imageUrl 제거)
        List<GroupDetailResponse.MemberDto> memberDtos = members.stream()
                .map(gm -> new GroupDetailResponse.MemberDto(
                        gm.getGroupMemberSeq(),
                        gm.getUser().getUserPk(),
                        gm.getUser().getName(),
                        gm.getIsLeader()
                ))
                .toList();

        // HEALTH 그룹인 경우 목표 기준 조회
        GoalCriteriaDto goalCriteria = null;
        if (group.getType() == GroupType.HEALTH) {
            goalCriteriaRepository.findByGroup(group)
                    .ifPresent(criteria -> {
                        // 여기서 goalCriteria 변수에 할당하려면 final 문제로 다른 방식 필요
                    });
        }

        // 더 간단한 방식으로 변경
        GoalCriteriaDto finalGoalCriteria = null;
        if (group.getType() == GroupType.HEALTH) {
            Optional<GroupGoalCriteria> criteria = goalCriteriaRepository.findByGroup(group);
            if (criteria.isPresent()) {
                GroupGoalCriteria c = criteria.get();
                finalGoalCriteria = new GoalCriteriaDto(
                        c.getMinCalorie(),
                        c.getMinStep(),
                        c.getMinDistance(),
                        c.getMinDuration()
                );
            }
        }

        return new GroupDetailResponse(
                group.getGroupSeq(),
                group.getGroupName(),
                group.getGroupDescription(),
                group.getType(),
                group.getCapacity(),
                members.size(), // 현재 인원
                group.getCreatedAt(),
                finalGoalCriteria,
                memberDtos
        );
    }

    // 내 그룹 목록 조회
    public List<GroupResponse> getMyGroups(Long userId, GroupType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자가 속한 그룹 멤버 정보 조회 (N+1 방지용 쿼리 사용)
        List<GroupMember> groupMembers = groupMemberRepository.findByUserWithGroup(userId);

        return groupMembers.stream()
                .map(GroupMember::getGroup)  // Group 추출
                .filter(group -> type == null || group.getType() == type)  // 타입 필터링
                .map(group -> {
                    int currentMembers = groupMemberRepository.countByGroup(group);

                    return new GroupResponse(
                            group.getGroupSeq(),
                            group.getGroupName(),
                            group.getGroupDescription(),
                            group.getType(),
                            group.getCapacity(),
                            currentMembers,
                            group.getCreatedAt()
                    );
                })
                .toList();
    }

    // 그룹 검색 (이름으로)
    public List<GroupResponse> searchGroups(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "검색어를 입력해주세요");
        }

        List<Group> groups = groupRepository.findByGroupNameContaining(keyword.trim());

        return groups.stream()
                .map(group -> {
                    int currentMembers = groupMemberRepository.countByGroup(group);

                    return new GroupResponse(
                            group.getGroupSeq(),
                            group.getGroupName(),
                            group.getGroupDescription(),
                            group.getType(),
                            group.getCapacity(),
                            currentMembers,
                            group.getCreatedAt()
                    );
                })
                .toList();
    }

}
