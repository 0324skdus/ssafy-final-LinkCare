package com.ssafy.linkcare.group.service;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.group.dto.*;
import com.ssafy.linkcare.group.entity.*;
import com.ssafy.linkcare.group.enums.GroupType;
import com.ssafy.linkcare.group.enums.RequestStatus;
import com.ssafy.linkcare.group.repository.*;
import com.ssafy.linkcare.notification.enums.NotificationType;
import com.ssafy.linkcare.notification.service.NotificationService;
import com.ssafy.linkcare.s3.S3Service;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupService {

    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final NotificationService notificationService;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupInvitationRepository invitationRepository;
    private final GroupGoalCriteriaRepository goalCriteriaRepository;
    private final GroupJoinRequestRepository groupJoinRequestRepository;
    private final GroupHealthPermissionRepository healthPermissionRepository;
    private final GroupRequiredPermissionRepository requiredPermissionRepository;

    @Value("${app.default-group-image-url}")
    private String defaultGroupImageUrl;

    /*
        * 헬스 그룹 생성
            * - 그룹 생성
            * - 목표 기준 설정 (선택)
            * - 생성자를 방장으로 등록
            * - 필수 권한 설정 (걸음수, 심박수, 운동)
    */
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
                group.getImageUrl(),
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
                            group.getImageUrl(),
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
                            group.getImageUrl(),
                            group.getCreatedAt()
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
    public GroupResponse updateGroup(Long userId, Long groupSeq, UpdateGroupRequest request, String imageAction, MultipartFile newImage) {
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

        log.info("그룹 정보 수정 완료: groupSeq={}, userId={}", groupSeq, userId);

        // 5. 응답 생성
        int currentMembers = groupMemberRepository.countByGroup(group);
        return new GroupResponse(
                group.getGroupSeq(),
                group.getGroupName(),
                group.getGroupDescription(),
                group.getType(),
                group.getCapacity(),
                currentMembers,
                group.getImageUrl(),
                group.getCreatedAt()
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

        String notificationContent = user.getName() + "님이 가입 신청을 했습니다.";
        notificationService.createNotification(
                leader.getUser(),
                NotificationType.GROUP_JOIN_REQUEST,
                group.getGroupName(),
                notificationContent,
                group,
                joinRequest
        );

        log.info("그룹 참가 신청 완료 ({}): userId={}, groupSeq={}, agreedPermissions=[sleep={}, water={}, bp={}, bs={}]",
                joinMethod, user.getUserPk(), group.getGroupSeq(),
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

        log.info("참가 신청 승인 완료: requestSeq={}, userId={}, groupSeq={}",
                requestSeq, newUser.getUserPk(), group.getGroupSeq());
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
        String rejectionContent = "참가 신청이 거절되었습니다.";
        notificationService.createNotification(
                applicant,
                NotificationType.GROUP_JOIN_REJECTED,
                group.getGroupName(),
                rejectionContent,
                group,
                null
        );

        log.info("참가 신청 거절 완료: requestSeq={}, userId={}, groupSeq={}",
                requestSeq, joinRequest.getUser().getUserPk(), group.getGroupSeq());
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

}
