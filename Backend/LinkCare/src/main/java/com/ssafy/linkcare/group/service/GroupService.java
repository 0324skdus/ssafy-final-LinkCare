package com.ssafy.linkcare.group.service;

import com.ssafy.linkcare.character.entity.UserCharacter;
import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.gpt.dto.WeeklyHeaderResponse;
import com.ssafy.linkcare.group.dto.*;
import com.ssafy.linkcare.group.entity.*;
import com.ssafy.linkcare.group.enums.GroupType;
import com.ssafy.linkcare.group.enums.RequestStatus;
import com.ssafy.linkcare.group.event.GroupMemberChangedEvent;
import com.ssafy.linkcare.group.repository.*;
import com.ssafy.linkcare.group.scheduler.GroupHeaderScheduler;
import com.ssafy.linkcare.health.dto.*;
import com.ssafy.linkcare.health.service.HealthService;
import com.ssafy.linkcare.health.service.SleepService;
import com.ssafy.linkcare.health.service.StepService;
import com.ssafy.linkcare.notification.enums.NotificationType;
import com.ssafy.linkcare.notification.service.NotificationService;
import com.ssafy.linkcare.s3.S3Service;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final S3Service s3Service;
    private final StepService stepService;
    private final SleepService sleepService;
    private final HealthService healthService;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final NotificationService notificationService;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupInvitationRepository invitationRepository;
    private final GroupGoalCriteriaRepository goalCriteriaRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final GroupHealthPermissionRepository healthPermissionRepository;
    private final GroupRequiredPermissionRepository requiredPermissionRepository;
    private final GroupHeaderScheduler groupHeaderScheduler;
    private final com.ssafy.linkcare.notification.repository.NotificationRepository notificationRepository;

    private final ApplicationEventPublisher eventPublisher;

    // 마지막 재생성 시간 캐시
    private final Map<Long, LocalDateTime> lastRegenerateTime = new ConcurrentHashMap<>();

    @Value("${app.default-group-image-url}")
    private String defaultGroupImageUrl;

    @Transactional
    public GroupResponse createHealthGroup(Long userId, CreateHealthGroupRequest request, MultipartFile image) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 이미지 업로드 (있으면 업로드, 없으면 기본 이미지)
        String imageUrl = (image != null && !image.isEmpty())
                ? s3Service.uploadGroupImage(image)
                : defaultGroupImageUrl;

        // 3. 그룹 생성
        Group group = Group.builder()
                .groupName(request.groupName())
                .groupDescription(request.groupDescription())
                .type(GroupType.HEALTH)
                .capacity(request.capacity())
                .imageUrl(imageUrl)
                .build();
        groupRepository.save(group);

        // 4. 목표 기준 생성 (있는 경우만)
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

        // 5. 생성자를 방장으로 등록
        GroupMember leader = GroupMember.builder()
                .group(group)
                .user(user)
                .isLeader(true)
                .build();
        groupMemberRepository.save(leader);

        // 6. 필수 권한 설정 (헬스 그룹: 걸음수, 심박수, 운동 필수)
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

        // 7. 응답 생성
        return new GroupResponse(
                group.getGroupSeq(),
                group.getGroupName(),
                group.getGroupDescription(),
                group.getType(),
                group.getCapacity(),
                1,  // 현재 멤버 수 (방장 1명)
                group.getImageUrl(),
                group.getCreatedAt(),
                "MEMBER"  // 생성자는 자동으로 멤버
        );
    }

    /*
     * 케어 그룹 생성
     * - 그룹 생성 (목표 기준 없음)
     * - 생성자를 방장으로 등록
     * - 권한 설정 (필수 + 선택 동의 항목)
     */
    @Transactional
    public GroupResponse createCareGroup(Long userId, CreateCareGroupRequest request, MultipartFile image) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 이미지 업로드 (있으면 업로드, 없으면 기본 이미지)
        String imageUrl = (image != null && !image.isEmpty())
                ? s3Service.uploadGroupImage(image)
                : defaultGroupImageUrl;

        // 3. 그룹 생성
        Group group = Group.builder()
                .groupName(request.groupName())
                .groupDescription(request.groupDescription())
                .type(GroupType.CARE)
                .capacity(request.capacity())
                .imageUrl(imageUrl)
                .build();
        groupRepository.save(group);

        // 4. 생성자를 방장으로 등록
        GroupMember leader = GroupMember.builder()
                .group(group)
                .user(user)
                .isLeader(true)
                .build();
        groupMemberRepository.save(leader);

        // 5. 그룹 요구 권한 설정 (CARE 그룹만)
        HealthPermissionDto permissions = request.permissions() != null
                ? request.permissions()
                : new HealthPermissionDto();  // 기본값 (선택 항목 모두 false)

        // 5-1. 그룹이 요구하는 권한 저장
        GroupRequiredPermission requiredPermission = GroupRequiredPermission.builder()
                .group(group)
                .isSleepRequired(permissions.isSleepAllowed())
                .isWaterIntakeRequired(permissions.isWaterIntakeAllowed())
                .isBloodPressureRequired(permissions.isBloodPressureAllowed())
                .isBloodSugarRequired(permissions.isBloodSugarAllowed())
                .build();
        requiredPermissionRepository.save(requiredPermission);

        // 5-2. 방장의 권한 설정 (그룹 요구사항과 동일)
        GroupHealthPermission leaderPermission = GroupHealthPermission.builder()
                .groupMember(leader)
                // 필수 항목 (항상 true)
                .isDailyStepAllowed(true)
                .isHeartRateAllowed(true)
                .isExerciseAllowed(true)
                // 선택 항목 (그룹 요구사항과 동일)
                .isSleepAllowed(requiredPermission.getIsSleepRequired())
                .isWaterIntakeAllowed(requiredPermission.getIsWaterIntakeRequired())
                .isBloodPressureAllowed(requiredPermission.getIsBloodPressureRequired())
                .isBloodSugarAllowed(requiredPermission.getIsBloodSugarRequired())
                .build();
        healthPermissionRepository.save(leaderPermission);

        log.info("케어 그룹 생성 완료: groupSeq={}, userId={}", group.getGroupSeq(), userId);

        // 6. 응답 생성
        return new GroupResponse(
                group.getGroupSeq(),
                group.getGroupName(),
                group.getGroupDescription(),
                group.getType(),
                group.getCapacity(),
                1,  // 현재 멤버 수 (방장 1명)
                group.getImageUrl(),
                group.getCreatedAt(),
                "MEMBER"  // 생성자는 자동으로 멤버
        );
    }

    // 그룹 상세 조회 (멤버, 목표 기준, 권한 포함)
    public GroupDetailResponse getGroupDetail(Long groupSeq, Long currentUserSeq) {
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 그룹 멤버 조회 (N+1 방지 - 추후 최적화 가능)
        List<GroupMember> members = groupMemberRepository.findByGroup(group);

        // 멤버 DTO 변환
        List<GroupDetailResponse.MemberDto> memberDtos = members.stream()
                .map(gm -> {
                    User user = gm.getUser();
                    String baseImageUrl = null;
                    UserCharacter mainCharacter = user.getMainCharacter();

                    if (mainCharacter != null && mainCharacter.getCharacter() != null) {
                        baseImageUrl = s3Service.generatePresignedGetUrl(mainCharacter.getCharacter().getBaseImageS3Key());
                    }

                    return new GroupDetailResponse.MemberDto(
                            gm.getGroupMemberSeq(),
                            user.getUserPk(),
                            user.getName(),
                            gm.getIsLeader(),
                            baseImageUrl
                    );
                })
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
                group.getImageUrl(),
                group.getCreatedAt(),
                finalGoalCriteria,
                memberDtos,
                currentUserSeq  // 현재 사용자 ID 추가
        );
    }

    // 내 그룹 목록 조회
    @Transactional
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
                            group.getImageUrl(),
                            group.getCreatedAt(),
                            "MEMBER"  // 내 그룹은 항상 MEMBER
                    );
                })
                .toList();
    }

    @Transactional
    public MyGroupDetailResponseDto getGroupDetails(Long userId, Long groupSeq) {
        GroupMember groupMember = groupMemberRepository.findByUser_UserPkAndGroup_GroupSeq(userId, groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));
        boolean isLeader = groupMember.getIsLeader();

        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        List<GroupMember> members = groupMemberRepository.findMembersWithDetailsByGroup(group);

        List<GroupMemberProfileDto> memberProfiles = members.stream()
                .map(member -> {
                    User user = member.getUser();
                    String characterUrl = null;
                    String backgroundUrl = null;

                    if (user.getMainCharacter() != null) {
                        characterUrl = s3Service.generatePresignedGetUrl(user.getMainCharacter().getCharacter().getBaseImageS3Key());
                    }
                    if (user.getMainBackground() != null) {
                        backgroundUrl = s3Service.generatePresignedGetUrl(user.getMainBackground().getBackground().getS3Key());
                    }

                    return GroupMemberProfileDto.builder()
                            .userId(user.getUserPk())
                            .userName(user.getName())
                            .petName(user.getPetName())
                            .isLeader(isLeader)
                            .mainCharacterImageUrl(characterUrl)
                            .mainBackgroundImageUrl(backgroundUrl)
                            .build();
                })
                .collect(Collectors.toList());

        return MyGroupDetailResponseDto.builder()
                .groupId(group.getGroupSeq())
                .groupName(group.getGroupName())
                .members(memberProfiles)
                .build();
    }

    // 내가 신청한 그룹 목록 조회 (PENDING 상태)
    public List<GroupResponse> getMyPendingGroups(Long userId, GroupType type) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 사용자의 PENDING 상태 신청 목록 조회
        List<GroupJoinRequest> pendingRequests = groupJoinRequestRepository
                .findByUser_UserPkAndStatus(userId, RequestStatus.PENDING);

        return pendingRequests.stream()
                .map(GroupJoinRequest::getGroup)  // Group 추출
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
                            group.getImageUrl(),
                            group.getCreatedAt(),
                            "PENDING"  // 신청 대기 상태
                    );
                })
                .toList();
    }

    // 모든 그룹 목록 조회
    public List<GroupResponse> getAllGroups(Long userId, GroupType type) {
        List<Group> groups;
        if (type != null) {
            groups = groupRepository.findByType(type);
        } else {
            groups = groupRepository.findAll();
        }

        return groups.stream()
                .map(group -> {
                    int currentMembers = groupMemberRepository.countByGroup(group);

                    // 가입 상태 확인
                    String joinStatus = "NONE";
                    if (userId != null) {
                        // 이미 멤버인지 확인
                        boolean isMember = groupMemberRepository.existsByGroupAndUser_UserPk(group, userId);
                        if (isMember) {
                            joinStatus = "MEMBER";
                        } else {
                            // 대기 중인 신청이 있는지 확인
                            boolean hasPendingRequest = groupJoinRequestRepository
                                    .existsByGroup_GroupSeqAndUser_UserPkAndStatus(
                                            group.getGroupSeq(), userId, RequestStatus.PENDING
                                    );
                            if (hasPendingRequest) {
                                joinStatus = "PENDING";
                            }
                        }
                    }

                    return new GroupResponse(
                            group.getGroupSeq(),
                            group.getGroupName(),
                            group.getGroupDescription(),
                            group.getType(),
                            group.getCapacity(),
                            currentMembers,
                            group.getImageUrl(),
                            group.getCreatedAt(),
                            joinStatus
                    );
                })
                .toList();
    }

    // 그룹 검색 (이름으로)
    public List<GroupResponse> searchGroups(String keyword, Long userId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "검색어를 입력해주세요");
        }

        List<Group> groups = groupRepository.findByGroupNameContaining(keyword.trim());

        return groups.stream()
                .map(group -> {
                    int currentMembers = groupMemberRepository.countByGroup(group);

                    // 가입 상태 확인
                    String joinStatus = "NONE";
                    if (userId != null) {
                        // 이미 멤버인지 확인
                        boolean isMember = groupMemberRepository.existsByGroupAndUser_UserPk(group, userId);
                        if (isMember) {
                            joinStatus = "MEMBER";
                        } else {
                            // 대기 중인 신청이 있는지 확인
                            boolean hasPendingRequest = groupJoinRequestRepository
                                    .existsByGroup_GroupSeqAndUser_UserPkAndStatus(
                                            group.getGroupSeq(), userId, RequestStatus.PENDING
                                    );
                            if (hasPendingRequest) {
                                joinStatus = "PENDING";
                            }
                        }
                    }

                    return new GroupResponse(
                            group.getGroupSeq(),
                            group.getGroupName(),
                            group.getGroupDescription(),
                            group.getType(),
                            group.getCapacity(),
                            currentMembers,
                            group.getImageUrl(),
                            group.getCreatedAt(),
                            joinStatus
                    );
                })
                .toList();
    }

    /*
     * 그룹 정보 수정
     * - 방장만 수정 가능
     * - 그룹명, 소개, 이미지 수정 가능
     */
    @Transactional
    public GroupResponse updateGroup(Long userId, Long groupSeq, UpdateGroupRequest request, String
            imageAction, MultipartFile newImage) {
        // 1. 그룹 조회
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 방장 권한 확인
        GroupMember member = groupMemberRepository.findByGroupAndUser_UserPk(group, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (!member.getIsLeader()) {
            throw new CustomException(ErrorCode.NOT_GROUP_LEADER);
        }

        // 3. 이미지 처리 - imageAction에 따라 분기
        String newImageUrl = group.getImageUrl(); // 기본값: 기존 이미지

        switch (imageAction) {
            case "default":
                // 기본 이미지로 변경
                if (group.getImageUrl() != null && !group.getImageUrl().equals(defaultGroupImageUrl)) {
                    s3Service.deleteFile(group.getImageUrl()); // 기존 이미지 삭제
                }
                newImageUrl = defaultGroupImageUrl;
                log.info("그룹 이미지를 기본 이미지로 변경: groupSeq={}", groupSeq);
                break;

            case "upload":
                // 새 이미지 업로드
                if (newImage != null && !newImage.isEmpty()) {
                    String uploadedUrl = s3Service.uploadGroupImage(newImage);

                    // 기존 이미지 삭제 (기본 이미지가 아닌 경우만)
                    if (group.getImageUrl() != null && !group.getImageUrl().equals(defaultGroupImageUrl)) {
                        s3Service.deleteFile(group.getImageUrl());
                    }

                    newImageUrl = uploadedUrl;
                    log.info("그룹 이미지 업로드 완료: groupSeq={}, newUrl={}", groupSeq, uploadedUrl);
                } else {
                    log.warn("imageAction=upload이지만 이미지 파일이 없음. 기존 이미지 유지");
                }
                break;

            case "keep":
            default:
                // 기존 이미지 유지
                log.info("그룹 이미지 유지: groupSeq={}", groupSeq);
                break;
        }

        // 4. 그룹 정보 업데이트
        group.updateGroupInfo(request.groupName(), request.groupDescription());
        group.updateImageUrl(newImageUrl);

        // 5. 케어 그룹인 경우 권한 업데이트 및 알림 발송
        if (group.getType() == GroupType.CARE) {
            updateCareGroupPermissions(group, request);
        }

        // 6. 헬스 그룹인 경우 목표 기준 업데이트
        if (group.getType() == GroupType.HEALTH && request.goalCriteria() != null) {
            updateHealthGroupGoalCriteria(group, request.goalCriteria());
        }

        log.info("그룹 정보 수정 완료: groupSeq={}, userId={}", groupSeq, userId);

        // 6. 응답 생성
        int currentMembers = groupMemberRepository.countByGroup(group);
        return new GroupResponse(
                group.getGroupSeq(),
                group.getGroupName(),
                group.getGroupDescription(),
                group.getType(),
                group.getCapacity(),
                currentMembers,
                group.getImageUrl(),
                group.getCreatedAt(),
                "MEMBER"  // 수정 권한이 있다는 것은 멤버라는 의미
        );
    }

    /*
     * 초대 링크 생성
     * - 방장만 생성 가능
     * - 정원이 가득 차면 생성 불가
     * - 유효한 링크가 이미 있으면 재사용
     */
    @Transactional
    public InvitationResponse createInvitation(Long userId, Long groupSeq) {
        // 1. 그룹 조회
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 방장 권한 확인
        GroupMember member = groupMemberRepository.findByGroupAndUser_UserPk(group, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (!member.getIsLeader()) {
            throw new CustomException(ErrorCode.NOT_GROUP_LEADER);
        }

        // 3. 정원 체크 (핵심!)
        int currentMembers = groupMemberRepository.countByGroup(group);
        if (currentMembers >= group.getCapacity()) {
            throw new CustomException(ErrorCode.GROUP_CAPACITY_FULL);
        }

        // 4. 기존 유효한 링크가 있으면 재사용
        Optional<GroupInvitation> existingInvitation =
                invitationRepository.findValidInvitationByGroup(group, LocalDateTime.now());

        if (existingInvitation.isPresent()) {
            log.info("기존 유효한 초대 링크 재사용: groupSeq={}, token={}", groupSeq, existingInvitation.get().getInvitationToken());
            return toInvitationResponse(existingInvitation.get());
        }

        // 5. 새 초대 링크 생성
        GroupInvitation invitation = GroupInvitation.builder()
                .group(group)
                .build();

        invitationRepository.save(invitation);

        log.info("새 초대 링크 생성 완료: groupSeq={}, token={}", groupSeq, invitation.getInvitationToken());

        return toInvitationResponse(invitation);
    }

    /*
     * 그룹 참가 신청 공통 로직
     * - 중복 체크 (이미 멤버, 이미 신청)
     * - 정원 체크
     * - 권한 동의 검증 (사용자가 방장보다 많은 권한 요구 불가)
     * - GroupJoinRequest 생성
     */
    private void createJoinRequest(User user, Group group, PermissionAgreementDto agreement, String joinMethod) {
        // 1. 이미 그룹 멤버인지 체크
        boolean alreadyMember = groupMemberRepository.findByGroupAndUser_UserPk(group, user.getUserPk())
                .isPresent();

        if (alreadyMember) {
            throw new CustomException(ErrorCode.ALREADY_GROUP_MEMBER);
        }

        // 2. 이미 대기 중인 신청이 있는지 체크 (중복 신청 방지)
        boolean alreadyRequested = groupJoinRequestRepository.existsByGroupAndUserAndStatus(
                group, user, RequestStatus.PENDING);

        if (alreadyRequested) {
            throw new CustomException(ErrorCode.DUPLICATE_JOIN_REQUEST);
        }

        // 3. 정원 체크
        int currentMembers = groupMemberRepository.countByGroup(group);
        if (currentMembers >= group.getCapacity()) {
            throw new CustomException(ErrorCode.GROUP_CAPACITY_FULL);
        }

        // 4. 그룹이 요구하는 권한 조회 및 검증
        if (group.getType() == GroupType.CARE) {
            // CARE 그룹: GroupRequiredPermission에서 요구사항 조회
            GroupRequiredPermission required = requiredPermissionRepository.findByGroup(group)
                    .orElseThrow(() -> new CustomException(ErrorCode.GROUP_REQUIRED_PERMISSION_NOT_FOUND));

            // 5. 권한 동의 검증: 그룹이 요구하는 권한을 모두 동의했는지 확인
            if (required.getIsSleepRequired() && !agreement.isSleepAllowed()) {
                throw new CustomException(ErrorCode.PERMISSION_AGREEMENT_INVALID,
                        "이 그룹은 수면 데이터 공유가 필수입니다");
            }
            if (required.getIsWaterIntakeRequired() && !agreement.isWaterIntakeAllowed()) {
                throw new CustomException(ErrorCode.PERMISSION_AGREEMENT_INVALID,
                        "이 그룹은 물 섭취량 공유가 필수입니다");
            }
            if (required.getIsBloodPressureRequired() && !agreement.isBloodPressureAllowed()) {
                throw new CustomException(ErrorCode.PERMISSION_AGREEMENT_INVALID,
                        "이 그룹은 혈압 데이터 공유가 필수입니다");
            }
            if (required.getIsBloodSugarRequired() && !agreement.isBloodSugarAllowed()) {
                throw new CustomException(ErrorCode.PERMISSION_AGREEMENT_INVALID,
                        "이 그룹은 혈당 데이터 공유가 필수입니다");
            }
        }
        // HEALTH 그룹은 선택 권한이 없으므로 검증 불필요

        // 6. 참가 신청 생성 (대기열 등록 + 동의한 권한 저장)
        GroupJoinRequest joinRequest = GroupJoinRequest.builder()
                .group(group)
                .user(user)
                .agreedSleep(agreement.isSleepAllowed())
                .agreedWaterIntake(agreement.isWaterIntakeAllowed())
                .agreedBloodPressure(agreement.isBloodPressureAllowed())
                .agreedBloodSugar(agreement.isBloodSugarAllowed())
                .build();

        groupJoinRequestRepository.save(joinRequest);

        // 7. 방장 찾기 및 알림 전송
        GroupMember leader = groupMemberRepository.findByGroup(group).stream()
                .filter(GroupMember::getIsLeader)
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        log.info("방장 찾기 완료: leaderId={}, leaderName={}", leader.getUser().getUserPk(), leader.getUser().getName());

        String notificationContent = user.getName() + "님이 가입 신청을 했습니다.";
        log.info("알림 생성 시작: 방장={}에게, 신청자={}, 그룹={}",
                leader.getUser().getName(), user.getName(), group.getGroupName());

        notificationService.createNotification(
                leader.getUser(),
                NotificationType.GROUP_JOIN_REQUEST,
                group.getGroupName(),
                notificationContent,
                group,
                joinRequest
        );

        log.info("✅ 그룹 참가 신청 완료 ({}): 신청자userId={}, 방장userId={}, groupSeq={}, agreedPermissions=[sleep={}, water={}, bp={}, bs={}]",
                joinMethod, user.getUserPk(), leader.getUser().getUserPk(), group.getGroupSeq(),
                agreement.isSleepAllowed(), agreement.isWaterIntakeAllowed(),
                agreement.isBloodPressureAllowed(), agreement.isBloodSugarAllowed());
    }

    /*
     * 초대 링크로 그룹 정보 미리보기
     * - 토큰으로 그룹 정보 조회
     * - 만료 여부, 정원 초과 여부 체크
     * - 방장의 권한 설정 조회 (참가자가 동의해야 할 항목)
     * - 사용자가 참여 버튼을 누를지 결정하도록 정보 제공
     */
    public InvitationPreviewResponse getInvitationPreview(String invitationToken) {
        // 1. 토큰으로 초대 링크 조회
        GroupInvitation invitation = invitationRepository.findByInvitationToken(invitationToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITATION_NOT_FOUND));

        // 2. 그룹 정보 조회
        Group group = invitation.getGroup();

        // 3. 현재 인원 조회
        int currentMembers = groupMemberRepository.countByGroup(group);

        // 4. 만료 여부 체크
        boolean isExpired = invitation.getExpiredAt().isBefore(LocalDateTime.now())
                || invitation.getUsedAt() != null;

        // 5. 정원 초과 여부 체크
        boolean isFull = currentMembers >= group.getCapacity();

        // 6. 필수 권한 (항상 true)
        InvitationPreviewResponse.RequiredPermissions requiredPermissions =
                new InvitationPreviewResponse.RequiredPermissions(
                        true,  // isDailyStepAllowed
                        true,  // isHeartRateAllowed
                        true   // isExerciseAllowed
                );

// 7. 선택 권한 (그룹이 요구하는 권한 조회)
        InvitationPreviewResponse.OptionalPermissions optionalPermissions;

        if (group.getType() == GroupType.CARE) {
            // CARE 그룹: GroupRequiredPermission에서 조회
            GroupRequiredPermission required = requiredPermissionRepository.findByGroup(group)
                    .orElseThrow(() -> new CustomException(ErrorCode.GROUP_REQUIRED_PERMISSION_NOT_FOUND));

            optionalPermissions = new InvitationPreviewResponse.OptionalPermissions(
                    required.getIsSleepRequired(),
                    required.getIsWaterIntakeRequired(),
                    required.getIsBloodPressureRequired(),
                    required.getIsBloodSugarRequired()
            );
        } else {
            // HEALTH 그룹: 선택 권한 없음 (모두 false)
            optionalPermissions = new InvitationPreviewResponse.OptionalPermissions(
                    false, false, false, false
            );
        }

        log.info("초대 링크 미리보기: token={}, groupSeq={}, isExpired={}, isFull={}",
                invitationToken, group.getGroupSeq(), isExpired, isFull);

        return new InvitationPreviewResponse(
                group.getGroupSeq(),
                group.getGroupName(),
                group.getGroupDescription(),
                group.getType(),
                group.getCapacity(),
                currentMembers,
                group.getImageUrl(),
                group.getCreatedAt(),
                isExpired,
                isFull,
                invitationToken,
                requiredPermissions,
                optionalPermissions
        );
    }

    /*
     * 초대 링크로 그룹 참가 신청
     * - 토큰 검증 및 만료 체크
     * - 중복 방지 (이미 멤버 or 이미 신청)
     * - 정원 체크
     * - GroupJoinRequest 생성 (대기열 등록)
     * - 방장 승인 대기 상태
     */
    @Transactional
    public void joinByInvitation(Long userId, String invitationToken, PermissionAgreementDto agreement) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 토큰으로 초대 링크 조회
        GroupInvitation invitation = invitationRepository.findByInvitationToken(invitationToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVITATION_NOT_FOUND));

        // 3. 초대 링크 만료 체크
        if (invitation.getExpiredAt().isBefore(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.INVITATION_EXPIRED);
        }

        // 4. 그룹 조회
        Group group = invitation.getGroup();

        // 5. 공통 검증 및 참가 신청 생성
        createJoinRequest(user, group, agreement, "초대 링크");
    }

    /*
     * 검색으로 그룹 참가 신청 (초대 링크 없이)
     * - 그룹 존재 여부 확인
     * - 중복 방지 (이미 멤버 or 이미 신청)
     * - 정원 체크
     * - GroupJoinRequest 생성 (대기열 등록)
     * - 방장 승인 대기 상태
     */
    @Transactional
    public void joinBySearch(Long userId, Long groupSeq, PermissionAgreementDto agreement) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 2. 그룹 조회
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 3. 공통 검증 및 참가 신청 생성
        createJoinRequest(user, group, agreement, "검색");
    }

    /*
     * 그룹 참가 신청 공통 로직
     * - 중복 체크 (이미 멤버, 이미 신청)
     * - 정원 체크
     * - GroupJoinRequest 생성
     */
    private void createJoinRequest(User user, Group group, String joinMethod) {
        // 1. 이미 그룹 멤버인지 체크
        boolean alreadyMember = groupMemberRepository.findByGroupAndUser_UserPk(group, user.getUserPk())
                .isPresent();

        if (alreadyMember) {
            throw new CustomException(ErrorCode.ALREADY_GROUP_MEMBER);
        }

        // 2. 이미 대기 중인 신청이 있는지 체크 (중복 신청 방지)
        boolean alreadyRequested = groupJoinRequestRepository.existsByGroupAndUserAndStatus(
                group, user, RequestStatus.PENDING);

        if (alreadyRequested) {
            throw new CustomException(ErrorCode.DUPLICATE_JOIN_REQUEST);
        }

        // 3. 정원 체크
        int currentMembers = groupMemberRepository.countByGroup(group);
        if (currentMembers >= group.getCapacity()) {
            throw new CustomException(ErrorCode.GROUP_CAPACITY_FULL);
        }

        // 4. 참가 신청 생성 (대기열 등록)
        GroupJoinRequest joinRequest = GroupJoinRequest.builder()
                .group(group)
                .user(user)
                .build();

        groupJoinRequestRepository.save(joinRequest);

        log.info("그룹 참가 신청 완료 ({}): userId={}, groupSeq={}", joinMethod, user.getUserPk(), group.getGroupSeq());
    }

    /*
     * 방장이 대기 중인 참가 신청 목록 조회
     * - 방장 권한 확인
     * - PENDING 상태인 신청만 조회
     */
    public List<JoinRequestResponse> getPendingJoinRequests(Long userId, Long groupSeq) {
        // 1. 그룹 조회
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 방장 권한 확인
        GroupMember member = groupMemberRepository.findByGroupAndUser_UserPk(group, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (!member.getIsLeader()) {
            throw new CustomException(ErrorCode.NOT_GROUP_LEADER);
        }

        // 3. 대기 중인 신청 목록 조회
        List<GroupJoinRequest> requests = groupJoinRequestRepository.findByGroup_GroupSeqAndStatus(
                groupSeq, RequestStatus.PENDING);

        // 4. DTO 변환
        return requests.stream()
                .map(request -> {
                    User user = request.getUser();
                    Integer age = user.getBirth() != null
                            ? LocalDateTime.now().getYear() - user.getBirth().getYear() + 1
                            : null;

                    return new JoinRequestResponse(
                            request.getRequestSeq(),
                            user.getUserPk(),
                            user.getName(),
                            age,
                            user.getGender(),
                            request.getStatus(),
                            request.getRequestedAt()
                    );
                })
                .toList();
    }

    /*
     * 참가 신청 승인
     * - 방장 권한 확인
     * - 정원 체크 (승인 시점에 다시 확인)
     * - GroupMember 생성 (실제 멤버 추가)
     * - GroupHealthPermission 생성 (방장 권한 복사)
     * - 신청 상태를 APPROVED로 변경
     */
    @Transactional
    public void approveJoinRequest(Long userId, Long requestSeq) {
        // 1. 참가 신청 조회
        GroupJoinRequest joinRequest = groupJoinRequestRepository.findById(requestSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        // 2. 그룹 조회
        Group group = joinRequest.getGroup();

        // 3. 방장 권한 확인
        GroupMember leader = groupMemberRepository.findByGroupAndUser_UserPk(group, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (!leader.getIsLeader()) {
            throw new CustomException(ErrorCode.NOT_GROUP_LEADER);
        }

        // 4. 이미 처리된 신청인지 확인
        if (joinRequest.getStatus() != RequestStatus.PENDING) {
            throw new CustomException(ErrorCode.JOIN_REQUEST_ALREADY_PROCESSED);
        }

        // 5. 정원 체크 (승인 시점에 다시 확인 - 동시성 고려)
        int currentMembers = groupMemberRepository.countByGroup(group);
        if (currentMembers >= group.getCapacity()) {
            throw new CustomException(ErrorCode.GROUP_CAPACITY_EXCEEDED);
        }

        // 6. 그룹 멤버로 추가
        User newUser = joinRequest.getUser();
        GroupMember newMember = GroupMember.builder()
                .group(group)
                .user(newUser)
                .isLeader(false)
                .build();
        groupMemberRepository.save(newMember);

        /// 7. 권한 설정 (신청자가 동의한 권한으로 생성)
        GroupHealthPermission newPermission = GroupHealthPermission.builder()
                .groupMember(newMember)
                // 필수 권한 (항상 true)
                .isDailyStepAllowed(true)
                .isHeartRateAllowed(true)
                .isExerciseAllowed(true)
                // 선택 권한 (신청자가 동의한 값 사용)
                .isSleepAllowed(joinRequest.getAgreedSleep())
                .isWaterIntakeAllowed(joinRequest.getAgreedWaterIntake())
                .isBloodPressureAllowed(joinRequest.getAgreedBloodPressure())
                .isBloodSugarAllowed(joinRequest.getAgreedBloodSugar())
                .build();
        healthPermissionRepository.save(newPermission);

        // 8. 신청 상태를 APPROVED로 변경
        joinRequest.approve();

        // 9. 신청자에게 승인 알림 전송
        String approvalContent = "참가 신청이 승인되었습니다.";
        notificationService.createNotification(
                newUser,
                NotificationType.GROUP_JOIN_APPROVED,
                group.getGroupName(),
                approvalContent,
                group,
                null
        );

        // 이벤트 발행
        log.info("그룹원 추가 이벤트 발행 시작: 그룹={}, 유저={}",
                joinRequest.getUser().getUserPk(), joinRequest.getGroup().getGroupSeq());

        eventPublisher.publishEvent(
                new GroupMemberChangedEvent(
                        joinRequest.getGroup().getGroupSeq(),
                        "ADDED",
                        joinRequest.getUser().getUserPk()
                )
        );
    }

    /*
     * 참가 신청 거절
     * - 방장 권한 확인
     * - 신청 상태를 REJECTED로 변경
     */
    @Transactional
    public void rejectJoinRequest(Long userId, Long requestSeq) {
        // 1. 참가 신청 조회
        GroupJoinRequest joinRequest = groupJoinRequestRepository.findById(requestSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.JOIN_REQUEST_NOT_FOUND));

        // 2. 그룹 조회
        Group group = joinRequest.getGroup();

        // 3. 방장 권한 확인
        GroupMember leader = groupMemberRepository.findByGroupAndUser_UserPk(group, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (!leader.getIsLeader()) {
            throw new CustomException(ErrorCode.NOT_GROUP_LEADER);
        }

        // 4. 이미 처리된 신청인지 확인
        if (joinRequest.getStatus() != RequestStatus.PENDING) {
            throw new CustomException(ErrorCode.JOIN_REQUEST_ALREADY_PROCESSED);
        }

        // 5. 신청 거절 처리
        joinRequest.reject();

        // 6. 신청자에게 거절 알림 전송
        User applicant = joinRequest.getUser();
        log.info("🔍 거절 알림 전송 대상 확인: 그룹장userId={}, 신청자userId={}, 신청자이름={}",
                userId, applicant.getUserPk(), applicant.getName());

        String rejectionContent = "참가 신청이 거절되었습니다.";
        notificationService.createNotification(
                applicant,
                NotificationType.GROUP_JOIN_REJECTED,
                group.getGroupName(),
                rejectionContent,
                group,
                null
        );

        log.info("참가 신청 거절 완료: requestSeq={}, 신청자userId={}, groupSeq={}",
                requestSeq, joinRequest.getUser().getUserPk(), group.getGroupSeq());
    }

    /**
     * 저장된 주간 헤더 조회
     */
    public WeeklyHeaderResponse getWeeklyHeader(Long groupSeq) {
        Group group = validateGroup(groupSeq);

        // 헤더가 아직 생성되지 않았으면 기본 메시지
        if (group.getWeeklyHeaderMessage() == null) {
            return WeeklyHeaderResponse.builder()
                    .headerMessage("이번 주도 함께 달려봐요! 💪")
                    .generatedAt(LocalDateTime.now())
                    .build();
        }

        return WeeklyHeaderResponse.builder()
                .headerMessage(group.getWeeklyHeaderMessage())
                .generatedAt(group.getHeaderGeneratedAt())
                .build();
    }

    /**
     * 헤더 수동 재생성 (30초 중복 방지)
     */
    public WeeklyHeaderResponse regenerateWeeklyHeader(Long groupSeq) {
        log.info("헤더 재생성 요청: 그룹={}", groupSeq);

        // 30초 이내 재생성 요청 차단
        LocalDateTime lastTime = lastRegenerateTime.get(groupSeq);
        if (lastTime != null && lastTime.isAfter(LocalDateTime.now().minusSeconds(30))) {
            long secondsLeft = 30 - java.time.Duration.between(lastTime, LocalDateTime.now()).getSeconds();
            log.warn("그룹 {} - 30초 이내 재생성 요청 차단, 남은 시간: {}초", groupSeq, secondsLeft);

            throw new CustomException(ErrorCode.TOO_MANY_REQUESTS);
        }

        // 재생성 실행
        log.info("그룹 {} 헤더 재생성 시작", groupSeq);
        WeeklyHeaderResponse response = groupHeaderScheduler.updateGroupHeader(groupSeq);

        // 마지막 재생성 시간 기록
        lastRegenerateTime.put(groupSeq, LocalDateTime.now());
        log.info("그룹 {} 헤더 재생성 완료", groupSeq);

        return response;
    }

    // 그룹 건강 요약 데이터 반환
    public List<HealthStaticsResponse> getLastWeekGroupHealthStats(Long groupSeq, LocalDate startDate, LocalDate
            endDate) {
        Group group = validateGroup(groupSeq);

        List<GroupMember> members = groupMemberRepository
                .findByGroupWithUser(group);  // fetch join user

        return members.stream()
                .map(member -> {
                    try {
                        return healthService.getHealthStaticsData(
                                Math.toIntExact(member.getUser().getUserPk()),
                                startDate,
                                endDate
                        );
                    } catch (Exception e) {
                        log.error("Failed to get health data for user: {}",
                                member.getUser().getUserPk(), e);
                        return null;  // 또는 기본값
                    }
                })
                .filter(Objects::nonNull)  // null 제거
                .toList();
    }

    // 그룹원 전체 총 걸음 수, 총 칼로리, 총 운동 시간 조회
    public TotalActivityStatisticsResponse getGroupTotalActivityStatistics(Long groupSeq, LocalDate
            startDate, LocalDate endDate) {
        Group group = validateGroup(groupSeq);

        List<GroupMember> members = groupMemberRepository.findByGroupWithUser(group);

        // 초기값
        double totalCalories = 0.0;
        long totalSteps = 0L;
        long totalDuration = 0L;

        // 각 멤버의 데이터 합산
        for (GroupMember member : members) {
            TotalActivityStatisticsResponse stats = healthService.getTotalActivityStatistics(
                    Math.toIntExact(member.getUser().getUserPk()),
                    startDate,
                    endDate
            );

            if (stats != null) {
                totalCalories += (stats.getTotalCalories() != null ? stats.getTotalCalories() : 0.0);
                totalSteps += (stats.getTotalSteps() != null ? stats.getTotalSteps() : 0L);
                totalDuration += (stats.getTotalDuration() != null ? stats.getTotalDuration() : 0L);
            }
        }

        return TotalActivityStatisticsResponse.builder()
                .totalCalories(totalCalories)
                .totalSteps(totalSteps)
                .totalDuration(totalDuration)
                .build();
    }

    // 그룹원 수면 기록 조회
    public GroupSleepStatisticsResponse getGroupSleepStatistics(Long groupSeq, LocalDate startDate, LocalDate
            endDate) {
        Group group = validateGroup(groupSeq);

        // 2. 그룹의 모든 멤버 조회
        List<GroupMember> groupMembers = groupMemberRepository.findByGroup(group);

        // 3. 각 멤버의 수면 통계 조회
        List<SleepDetailResponse> members = groupMembers.stream()
                .map(groupMember -> sleepService.getSleepStatisticsForGroup(
                        Math.toIntExact(groupMember.getUser().getUserPk()),
                        startDate,
                        endDate))
                .collect(Collectors.toList());

        // 4. 그룹 전체 평균 수면시간 계산
        Long groupAvgDuration = (long) members.stream()
                .mapToLong(SleepDetailResponse::getAverageSleepMinutes)
                .average()
                .orElse(0.0);


        // 4. 응답 생성
        return GroupSleepStatisticsResponse.builder()
                .members(members)
                .avgDuration(groupAvgDuration)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    // 당일 그룹원별 걸음수, 그룹원 총 걸음수 조회
    public GroupStepStatisticsResponse getGroupStepStatistics(Long groupSeq) {
        Group group = validateGroup(groupSeq);

        List<GroupMember> groupMembers = groupMemberRepository.findByGroup(group);

        List<StepDetailResponse> responses = groupMembers.stream()
                .map(groupMember -> {
                    int userSeq = Math.toIntExact(groupMember.getUser().getUserPk());
                    int steps = stepService.getTodayStepCount(userSeq);
                    return StepDetailResponse.builder()
                            .userSeq(userSeq)
                            .steps(steps)
                            .build();
                })
                .toList();

        int totalSteps = responses.stream().mapToInt(StepDetailResponse::getSteps).sum();

        return GroupStepStatisticsResponse.builder()
                .members(responses)
                .totalSteps(totalSteps)
                .build();
    }

    // 특정 날짜 그룹원별 걸음수, 그룹원 총 걸음수 조회
    public GroupStepStatisticsResponse getGroupStepStatisticsByDate(Long groupSeq, LocalDate date) {
        Group group = validateGroup(groupSeq);

        List<GroupMember> groupMembers = groupMemberRepository.findByGroup(group);

        List<StepDetailResponse> responses = groupMembers.stream()
                .map(groupMember -> {
                    int userSeq = Math.toIntExact(groupMember.getUser().getUserPk());
                    int steps = stepService.getStepsByDate(userSeq, date);
                    return StepDetailResponse.builder()
                            .userSeq(userSeq)
                            .steps(steps)
                            .build();
                })
                .toList();

        int totalSteps = responses.stream().mapToInt(StepDetailResponse::getSteps).sum();

        return GroupStepStatisticsResponse.builder()
                .members(responses)
                .totalSteps(totalSteps)
                .build();
    }


    // 특정 기간 그룹원별 걸음수, 그룹원 총 걸음수 조회
    public GroupStepStatisticsResponse getGroupStepStatisticsByPeriod(Long groupSeq, LocalDate startDate, LocalDate endDate) {
        Group group = validateGroup(groupSeq);

        List<GroupMember> groupMembers = groupMemberRepository.findByGroup(group);

        List<StepDetailResponse> responses = groupMembers.stream()
                .map(groupMember -> {
                    int userSeq = Math.toIntExact(groupMember.getUser().getUserPk());
                    StepStatisticsResponse stepStatisticsResponse = stepService.getStepStatisticsByDate(userSeq, startDate, endDate);
                    return StepDetailResponse.builder()
                            .userSeq(userSeq)
                            .steps(Math.toIntExact(stepStatisticsResponse.getTotalSteps()))
                            .build();
                })
                .toList();

        int totalSteps = responses.stream().mapToInt(StepDetailResponse::getSteps).sum();

        return GroupStepStatisticsResponse.builder()
                .members(responses)
                .totalSteps(totalSteps)
                .build();
    }

    /*
     * GroupInvitation을 InvitationResponse로 변환
     * invitationUrl은 null로 반환 - 프론트엔드에서 자유롭게 사용
     * 예시:
     *   - 앱 내 딥링크: linkcare://invite/{token}
     *   - 공유 메시지: "LinkCare 모임 초대! 앱에서 코드 입력: {token}"
     *   - 카카오톡 공유: 토큰 + 그룹명 조합
     */
    private InvitationResponse toInvitationResponse(GroupInvitation invitation) {
        return new InvitationResponse(
                invitation.getInvitationSeq(),
                invitation.getGroup().getGroupSeq(),
                invitation.getGroup().getGroupName(),
                invitation.getInvitationToken(),
                invitation.getCreatedAt(),
                invitation.getExpiredAt(),
                null  // 프론트엔드에서 직접 URL/메시지 구성
        );
    }

    private Group validateGroup(Long groupSeq) {
        return groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));
    }

    /*
     * 그룹원 권한 동의 처리
     * - 그룹원이 알림을 받고 동의 버튼을 누르면 호출
     * - GroupHealthPermission 업데이트
     */
    @Transactional
    public void agreeToGroupPermissions(Long userId, Long groupSeq, PermissionAgreementDto agreement) {
        // 1. 사용자 및 그룹 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 그룹 멤버 확인
        GroupMember member = groupMemberRepository.findByGroupAndUser_UserPk(group, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        // 3. 케어 그룹인지 확인
        if (group.getType() != GroupType.CARE) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "케어 그룹만 권한 동의가 가능합니다");
        }

        // 4. 그룹 요구 권한 조회
        GroupRequiredPermission required = requiredPermissionRepository.findByGroup(group)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_REQUIRED_PERMISSION_NOT_FOUND));

        // 5. 권한 동의 검증: 그룹이 요구하는 권한을 모두 동의했는지 확인
        if (required.getIsSleepRequired() && !agreement.isSleepAllowed()) {
            throw new CustomException(ErrorCode.PERMISSION_AGREEMENT_INVALID,
                    "이 그룹은 수면 데이터 공유가 필수입니다");
        }
        if (required.getIsWaterIntakeRequired() && !agreement.isWaterIntakeAllowed()) {
            throw new CustomException(ErrorCode.PERMISSION_AGREEMENT_INVALID,
                    "이 그룹은 물 섭취량 공유가 필수입니다");
        }
        if (required.getIsBloodPressureRequired() && !agreement.isBloodPressureAllowed()) {
            throw new CustomException(ErrorCode.PERMISSION_AGREEMENT_INVALID,
                    "이 그룹은 혈압 데이터 공유가 필수입니다");
        }
        if (required.getIsBloodSugarRequired() && !agreement.isBloodSugarAllowed()) {
            throw new CustomException(ErrorCode.PERMISSION_AGREEMENT_INVALID,
                    "이 그룹은 혈당 데이터 공유가 필수입니다");
        }

        // 6. 기존 권한 조회 및 업데이트
        GroupHealthPermission permission = healthPermissionRepository.findByGroupMember(member)
                .orElseThrow(() -> new CustomException(ErrorCode.HEALTH_PERMISSION_NOT_FOUND));

        // 7. 새로운 권한으로 업데이트 (Builder 패턴 사용)
        GroupHealthPermission updatedPermission = GroupHealthPermission.builder()
                .groupMember(member)
                // 필수 권한 (항상 true)
                .isDailyStepAllowed(true)
                .isHeartRateAllowed(true)
                .isExerciseAllowed(true)
                // 선택 권한 (사용자가 동의한 값)
                .isSleepAllowed(agreement.isSleepAllowed())
                .isWaterIntakeAllowed(agreement.isWaterIntakeAllowed())
                .isBloodPressureAllowed(agreement.isBloodPressureAllowed())
                .isBloodSugarAllowed(agreement.isBloodSugarAllowed())
                .build();

        // 기존 엔티티 삭제 후 새로 저장 (OneToOne 관계 업데이트)
        healthPermissionRepository.delete(permission);
        healthPermissionRepository.save(updatedPermission);

        log.info("그룹원 권한 동의 처리 완료: userId={}, groupSeq={}, sleep={}, water={}, bp={}, bs={}",
                userId, groupSeq,
                agreement.isSleepAllowed(), agreement.isWaterIntakeAllowed(),
                agreement.isBloodPressureAllowed(), agreement.isBloodSugarAllowed());
    }

    /*
     * 케어 그룹 권한 자동 동의 (그룹 요구사항에 맞춰 자동으로 동의)
     * - 권한 변경 알림 받은 후 간편하게 "동의" 버튼만 누르면 호출되는 API
     * - 그룹이 요구하는 모든 선택 권한을 자동으로 true로 설정
     */
    @Transactional
    public void autoAgreeToGroupPermissions(Long userId, Long groupSeq) {
        // 1. 사용자 및 그룹 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 그룹 멤버 확인
        GroupMember member = groupMemberRepository.findByGroupAndUser_UserPk(group, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        // 3. 케어 그룹인지 확인
        if (group.getType() != GroupType.CARE) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "케어 그룹만 권한 동의가 가능합니다");
        }

        // 4. 그룹 요구 권한 조회
        GroupRequiredPermission required = requiredPermissionRepository.findByGroup(group)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_REQUIRED_PERMISSION_NOT_FOUND));

        // 5. 기존 권한 조회
        GroupHealthPermission permission = healthPermissionRepository.findByGroupMember(member)
                .orElseThrow(() -> new CustomException(ErrorCode.HEALTH_PERMISSION_NOT_FOUND));

        // 6. 기존 권한을 그대로 수정 (delete + save 대신 update 사용)
        // - 필수 권한: 항상 true
        // - 선택 권한: 그룹이 요구하면 true, 요구 안 하면 기존 값 유지
        permission.updatePermissions(
                true,  // isDailyStepAllowed
                true,  // isHeartRateAllowed
                true,  // isExerciseAllowed
                required.getIsSleepRequired() ? true : permission.getIsSleepAllowed(),
                required.getIsWaterIntakeRequired() ? true : permission.getIsWaterIntakeAllowed(),
                required.getIsBloodPressureRequired() ? true : permission.getIsBloodPressureAllowed(),
                required.getIsBloodSugarRequired() ? true : permission.getIsBloodSugarAllowed()
        );

        log.info("그룹원 권한 자동 동의 처리 완료: userId={}, groupSeq={}, sleep={}, water={}, bp={}, bs={}",
                userId, groupSeq,
                permission.getIsSleepAllowed(), permission.getIsWaterIntakeAllowed(),
                permission.getIsBloodPressureAllowed(), permission.getIsBloodSugarAllowed());
    }

    /*
     * 케어 그룹 권한 업데이트 및 알림 발송
     * - GroupRequiredPermission 업데이트
     * - 변경사항 감지 (true↔false 변경된 항목)
     * - 모든 그룹원에게 알림 발송
     */
    private void updateCareGroupPermissions(Group group, UpdateGroupRequest request) {
        // 1. 기존 권한 조회
        GroupRequiredPermission requiredPermission = requiredPermissionRepository.findByGroup(group)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_REQUIRED_PERMISSION_NOT_FOUND));

        // 2. 변경사항 감지
        boolean sleepChanged = false;
        boolean waterIntakeChanged = false;
        boolean bloodPressureChanged = false;
        boolean bloodSugarChanged = false;

        boolean sleepRequiredNow = false;
        boolean waterIntakeRequiredNow = false;
        boolean bloodPressureRequiredNow = false;
        boolean bloodSugarRequiredNow = false;

        // 수면
        if (request.isSleepRequired() != null && !request.isSleepRequired().equals(requiredPermission.getIsSleepRequired())) {
            sleepChanged = true;
            sleepRequiredNow = request.isSleepRequired();
        }

        // 물 섭취량
        if (request.isWaterIntakeRequired() != null && !request.isWaterIntakeRequired().equals(requiredPermission.getIsWaterIntakeRequired())) {
            waterIntakeChanged = true;
            waterIntakeRequiredNow = request.isWaterIntakeRequired();
        }

        // 혈압
        if (request.isBloodPressureRequired() != null && !request.isBloodPressureRequired().equals(requiredPermission.getIsBloodPressureRequired())) {
            bloodPressureChanged = true;
            bloodPressureRequiredNow = request.isBloodPressureRequired();
        }

        // 혈당
        if (request.isBloodSugarRequired() != null && !request.isBloodSugarRequired().equals(requiredPermission.getIsBloodSugarRequired())) {
            bloodSugarChanged = true;
            bloodSugarRequiredNow = request.isBloodSugarRequired();
        }

        // 3. 변경사항이 있으면 업데이트 및 알림 발송
        if (sleepChanged || waterIntakeChanged || bloodPressureChanged || bloodSugarChanged) {
            // 3-1. GroupRequiredPermission 업데이트 (Builder 사용)
            GroupRequiredPermission updatedPermission = GroupRequiredPermission.builder()
                    .group(group)
                    .isSleepRequired(request.isSleepRequired() != null ? request.isSleepRequired() : requiredPermission.getIsSleepRequired())
                    .isWaterIntakeRequired(request.isWaterIntakeRequired() != null ? request.isWaterIntakeRequired() : requiredPermission.getIsWaterIntakeRequired())
                    .isBloodPressureRequired(request.isBloodPressureRequired() != null ? request.isBloodPressureRequired() : requiredPermission.getIsBloodPressureRequired())
                    .isBloodSugarRequired(request.isBloodSugarRequired() != null ? request.isBloodSugarRequired() : requiredPermission.getIsBloodSugarRequired())
                    .build();

            // 기존 엔티티 삭제 후 새로 저장 (@MapsId 사용으로 인해)
            requiredPermissionRepository.delete(requiredPermission);
            requiredPermissionRepository.save(updatedPermission);

            // 3-2. 알림 메시지 생성
            StringBuilder notificationMessage = new StringBuilder("그룹 권한 설정이 변경되었습니다.\n");

            if (sleepChanged) {
                if (sleepRequiredNow) {
                    notificationMessage.append("수면 데이터 공유가 필수로 변경되었습니다. 동의해주세요.\n");
                } else {
                    notificationMessage.append("수면 데이터 공유가 선택으로 변경되었습니다.\n");
                }
            }

            if (waterIntakeChanged) {
                if (waterIntakeRequiredNow) {
                    notificationMessage.append("물 섭취량 공유가 필수로 변경되었습니다. 동의해주세요.\n");
                } else {
                    notificationMessage.append("물 섭취량 공유가 선택으로 변경되었습니다.\n");
                }
            }

            if (bloodPressureChanged) {
                if (bloodPressureRequiredNow) {
                    notificationMessage.append("혈압 데이터 공유가 필수로 변경되었습니다. 동의해주세요.\n");
                } else {
                    notificationMessage.append("혈압 데이터 공유가 선택으로 변경되었습니다.\n");
                }
            }

            if (bloodSugarChanged) {
                if (bloodSugarRequiredNow) {
                    notificationMessage.append("혈당 데이터 공유가 필수로 변경되었습니다. 동의해주세요.\n");
                } else {
                    notificationMessage.append("혈당 데이터 공유가 선택으로 변경되었습니다.\n");
                }
            }

            // 3-3. 모든 그룹원에게 알림 발송 (방장 제외)
            List<GroupMember> members = groupMemberRepository.findByGroupWithUser(group);

            for (GroupMember member : members) {
                if (!member.getIsLeader()) {  // 방장은 제외
                    notificationService.createNotification(
                            member.getUser(),
                            NotificationType.GROUP_PERMISSION_CHANGED,
                            group.getGroupName(),
                            notificationMessage.toString(),
                            group,
                            null
                    );
                }
            }

            log.info("케어 그룹 권한 업데이트 완료 및 알림 발송: groupSeq={}, sleep={}, water={}, bp={}, bs={}",
                    group.getGroupSeq(), sleepChanged, waterIntakeChanged, bloodPressureChanged, bloodSugarChanged);
        }
    }

    /*
     * 헬스 그룹 목표 기준 업데이트 (private 메서드)
     * - 기존 목표 기준이 있으면 업데이트
     * - 없으면 새로 생성
     */
    private void updateHealthGroupGoalCriteria(Group group, GoalCriteriaDto criteria) {
        Optional<GroupGoalCriteria> existingCriteria = goalCriteriaRepository.findByGroup(group);

        if (existingCriteria.isPresent()) {
            // 기존 목표 기준 업데이트
            GroupGoalCriteria goalCriteria = existingCriteria.get();
            goalCriteria.updateGoalCriteria(
                    criteria.minCalorie(),
                    criteria.minStep(),
                    criteria.minDistance(),
                    criteria.minDuration()
            );
            log.info("헬스 그룹 목표 기준 업데이트 완료: groupSeq={}", group.getGroupSeq());
        } else {
            // 새로운 목표 기준 생성
            GroupGoalCriteria newCriteria = GroupGoalCriteria.builder()
                    .group(group)
                    .minCalorie(criteria.minCalorie())
                    .minStep(criteria.minStep())
                    .minDistance(criteria.minDistance())
                    .minDuration(criteria.minDuration())
                    .build();
            goalCriteriaRepository.save(newCriteria);
            log.info("헬스 그룹 목표 기준 생성 완료: groupSeq={}", group.getGroupSeq());
        }
    }

    // 그룹장 위임하기
    @Transactional
    public void delegateLeader(Long groupSeq, Long newLeaderUserSeq, Long currentUserSeq) {
        // 1. 그룹 조회
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 현재 사용자가 그룹장인지 확인
        GroupMember currentLeader = groupMemberRepository.findByGroupAndUser_UserPk(group, currentUserSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (!currentLeader.getIsLeader()) {
            throw new CustomException(ErrorCode.NOT_GROUP_LEADER);
        }

        // 3. 새 그룹장이 그룹 멤버인지 확인
        GroupMember newLeader = groupMemberRepository.findByGroupAndUser_UserPk(group, newLeaderUserSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        // 4. 그룹장 권한 이전
        currentLeader.updateLeaderStatus(false);
        newLeader.updateLeaderStatus(true);

        groupMemberRepository.save(currentLeader);
        groupMemberRepository.save(newLeader);

        log.info("그룹장 위임 완료: groupSeq={}, oldLeader={}, newLeader={}",
                groupSeq, currentUserSeq, newLeaderUserSeq);
    }

    // 그룹원 내보내기
    @Transactional
    public void kickMember(Long groupSeq, Long targetUserSeq, Long currentUserSeq) {
        // 1. 그룹 조회
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 현재 사용자가 그룹장인지 확인
        GroupMember currentUser = groupMemberRepository.findByGroupAndUser_UserPk(group, currentUserSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (!currentUser.getIsLeader()) {
            throw new CustomException(ErrorCode.NOT_GROUP_LEADER);
        }

        // 3. 내보낼 멤버 조회
        GroupMember targetMember = groupMemberRepository.findByGroupAndUser_UserPk(group, targetUserSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        // 4. 그룹장은 내보낼 수 없음
        if (targetMember.getIsLeader()) {
            throw new CustomException(ErrorCode.CANNOT_KICK_LEADER);
        }

        // 5. 연관된 GroupHealthPermission 먼저 삭제 (외래 키 제약 조건 위반 방지)
        healthPermissionRepository.findByGroupMember(targetMember).ifPresent(permission -> {
            healthPermissionRepository.delete(permission);
            log.info("그룹원의 건강 권한 삭제 완료: permissionSeq={}", permission.getPermissionSeq());
        });

        // 6. 그룹원 삭제
        groupMemberRepository.delete(targetMember);

        log.info("그룹원 내보내기 완료: groupSeq={}, kickedUser={}", groupSeq, targetUserSeq);
    }

    // 그룹 탈퇴
    @Transactional
    public void leaveGroup(Long groupSeq, Long currentUserSeq) {
        // 1. 그룹 조회
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 현재 사용자의 그룹 멤버 정보 조회
        GroupMember currentMember = groupMemberRepository.findByGroupAndUser_UserPk(group, currentUserSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        // 3. 그룹장이면서 다른 멤버가 있으면 탈퇴 불가
        if (currentMember.getIsLeader()) {
            long memberCount = groupMemberRepository.countByGroup(group);
            if (memberCount > 1) {
                throw new CustomException(ErrorCode.LEADER_CANNOT_LEAVE_WITH_MEMBERS);
            }
            // 그룹장 혼자만 남았으면 그룹 전체 삭제
            deleteGroupCompletely(group);
            log.info("그룹장 탈퇴로 그룹 삭제 완료: groupSeq={}, user={}", groupSeq, currentUserSeq);
        } else {
            // 일반 멤버는 바로 탈퇴
            // 연관된 GroupHealthPermission 먼저 삭제
            healthPermissionRepository.findByGroupMember(currentMember).ifPresent(permission -> {
                healthPermissionRepository.delete(permission);
                log.info("탈퇴 멤버의 건강 권한 삭제 완료: permissionSeq={}", permission.getPermissionSeq());
            });

            groupMemberRepository.delete(currentMember);
            log.info("그룹 탈퇴 완료: groupSeq={}, user={}", groupSeq, currentUserSeq);
        }
    }

    // 그룹 삭제 (그룹장 전용)
    @Transactional
    public void deleteGroup(Long groupSeq, Long currentUserSeq) {
        // 1. 그룹 조회
        Group group = groupRepository.findById(groupSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_NOT_FOUND));

        // 2. 현재 사용자가 그룹장인지 확인
        GroupMember currentMember = groupMemberRepository.findByGroupAndUser_UserPk(group, currentUserSeq)
                .orElseThrow(() -> new CustomException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (!currentMember.getIsLeader()) {
            throw new CustomException(ErrorCode.NOT_GROUP_LEADER);
        }

        // 3. 그룹 전체 삭제
        deleteGroupCompletely(group);
        log.info("그룹 삭제 완료: groupSeq={}, deletedBy={}", groupSeq, currentUserSeq);
    }

    // 그룹 완전 삭제 (private 메서드)
    private void deleteGroupCompletely(Group group) {
        // 1. 모든 그룹 멤버 조회
        List<GroupMember> members = groupMemberRepository.findByGroup(group);

        // 2. 각 멤버의 건강 권한 삭제
        for (GroupMember member : members) {
            healthPermissionRepository.findByGroupMember(member).ifPresent(permission -> {
                healthPermissionRepository.delete(permission);
            });
        }

        // 3. 모든 그룹 멤버 삭제
        groupMemberRepository.deleteAll(members);

        // 4. 그룹 필수 권한 삭제 (CARE 그룹인 경우)
        requiredPermissionRepository.findByGroup(group).ifPresent(permission -> {
            requiredPermissionRepository.delete(permission);
        });

        // 5. 그룹 목표 기준 삭제 (HEALTH 그룹인 경우)
        goalCriteriaRepository.findByGroup(group).ifPresent(criteria -> {
            goalCriteriaRepository.delete(criteria);
        });

        // 6. 초대 링크들 삭제
        List<GroupInvitation> invitations = invitationRepository.findByGroup(group);
        invitationRepository.deleteAll(invitations);

        // 7. 가입 신청들 삭제
        List<GroupJoinRequest> joinRequests = groupJoinRequestRepository.findByGroup(group);
        groupJoinRequestRepository.deleteAll(joinRequests);

        // 8. 알림들 삭제 (그룹과 연관된 모든 알림)
        List<com.ssafy.linkcare.notification.entity.Notification> notifications =
                notificationRepository.findByRelatedGroupSeq(group.getGroupSeq());
        notificationRepository.deleteAll(notifications);

        // 9. 그룹 삭제
        groupRepository.delete(group);

        log.info("그룹 완전 삭제 완료: groupSeq={}, members={}, invitations={}, joinRequests={}, notifications={}",
                group.getGroupSeq(), members.size(), invitations.size(), joinRequests.size(), notifications.size());
    }
}
