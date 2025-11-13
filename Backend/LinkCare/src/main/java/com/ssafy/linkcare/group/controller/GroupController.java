package com.ssafy.linkcare.group.controller;

import com.ssafy.linkcare.group.dto.*;
import com.ssafy.linkcare.group.enums.GroupType;
import com.ssafy.linkcare.group.service.GroupService;
import com.ssafy.linkcare.group.service.WeeklyStatsService;
import com.ssafy.linkcare.health.dto.TotalActivityStatisticsResponse;
import com.ssafy.linkcare.health.dto.WaterIntakeStatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "그룹", description = "헬스/케어 그룹 관련 API")
public class GroupController {

    private final GroupService groupService;
    private final WeeklyStatsService weeklyStatsService;

    @Operation(summary = "헬스 그룹 생성",
            description = "운동 목표 기반 헬스 그룹을 생성합니다. 최소 목표 기준(칼로리/걸음수/거리/시간)을 설정할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "헬스 그룹 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 항목 누락)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping(value = "/health", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GroupResponse> createHealthGroup(
            Authentication authentication,
            @RequestParam("groupName") String groupName,
            @RequestParam("groupDescription") String groupDescription,
            @RequestParam("capacity") Integer capacity,
            @RequestParam(value = "minCalorie", required = false) Float minCalorie,
            @RequestParam(value = "minStep", required = false) Integer minStep,
            @RequestParam(value = "minDistance", required = false) Float minDistance,
            @RequestParam(value = "minDuration", required = false) Integer minDuration,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        Long userId = Long.parseLong(authentication.getName());

        // DTO 생성
        GoalCriteriaDto goalCriteria = null;
        if (minCalorie != null || minStep != null || minDistance != null || minDuration != null) {
            goalCriteria = new GoalCriteriaDto(minCalorie, minStep, minDistance, minDuration);
        }
        CreateHealthGroupRequest request = new CreateHealthGroupRequest(groupName, groupDescription, capacity, goalCriteria);

        GroupResponse response = groupService.createHealthGroup(userId, request, image);
        log.info("헬스 그룹 생성 API 호출: userId={}, groupName={}", userId, groupName);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "케어 그룹 생성",
            description = "건강 데이터 공유 기반 케어 그룹을 생성합니다. 선택 동의 항목(수면/음수량/혈압/혈당)을 설정할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "케어 그룹 생성 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 항목 누락)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping(value = "/care", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GroupResponse> createCareGroup(
            Authentication authentication,
            @RequestParam("groupName") String groupName,
            @RequestParam("groupDescription") String groupDescription,
            @RequestParam("capacity") Integer capacity,
            @RequestParam(value = "isSleepAllowed", required = false, defaultValue = "false") Boolean isSleepAllowed,
            @RequestParam(value = "isWaterIntakeAllowed", required = false, defaultValue = "false") Boolean isWaterIntakeAllowed,
            @RequestParam(value = "isBloodPressureAllowed", required = false, defaultValue = "false") Boolean isBloodPressureAllowed,
            @RequestParam(value = "isBloodSugarAllowed", required = false, defaultValue = "false") Boolean isBloodSugarAllowed,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        Long userId = Long.parseLong(authentication.getName());

        // DTO 생성
        HealthPermissionDto permissions = new HealthPermissionDto(
                isSleepAllowed,
                isWaterIntakeAllowed,
                isBloodPressureAllowed,
                isBloodSugarAllowed
        );
        CreateCareGroupRequest request = new CreateCareGroupRequest(
                groupName,
                groupDescription,
                capacity,
                permissions
        );

        GroupResponse response = groupService.createCareGroup(userId, request, image);

        log.info("케어 그룹 생성 API 호출: userId={}, groupName={}", userId, groupName);

        return ResponseEntity.ok(response);
    }

    // 내 그룹 목록 조회
    @GetMapping("/my")
    @Operation(summary = "내 그룹 목록 조회", description = "내가 속한 그룹 목록을 조회합니다. type 파라미터로 HEALTH/CARE 필터링 가능")
    public ResponseEntity<List<GroupResponse>> getMyGroups(
            Authentication authentication,
            @RequestParam(required = false) GroupType type) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("내 그룹 목록 조회 요청: userId={}, type={}", userId, type);

        List<GroupResponse> response = groupService.getMyGroups(userId, type);
        return ResponseEntity.ok(response);
    }

    // 그룹 검색
    @GetMapping("/search")
    @Operation(summary = "그룹 검색", description = "그룹 이름으로 검색합니다")
    public ResponseEntity<List<GroupResponse>> searchGroups(
            @RequestParam String keyword) {

        log.info("그룹 검색 요청: keyword={}", keyword);
        List<GroupResponse> response = groupService.searchGroups(keyword);
        return ResponseEntity.ok(response);
    }

    // 그룹 상세 조회
    @GetMapping("/{groupSeq}")
    @Operation(summary = "그룹 상세 조회", description = "그룹의 상세 정보와 멤버 목록을 조회합니다")
    public ResponseEntity<GroupDetailResponse> getGroupDetail(
            @PathVariable Long groupSeq) {

        log.info("그룹 상세 조회 요청: groupSeq={}", groupSeq);
        GroupDetailResponse response = groupService.getGroupDetail(groupSeq);
        return ResponseEntity.ok(response);
    }

    // 그룹 정보 수정
    @Operation(summary = "그룹 정보 수정", description = "그룹 이름, 소개, 이미지를 수정합니다. 방장만 수정 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @PutMapping(value = "/{groupSeq}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GroupResponse> updateGroup(
            Authentication authentication,
            @PathVariable Long groupSeq,
            @RequestParam("groupName") String groupName,
            @RequestParam("groupDescription") String groupDescription,
            @RequestParam(value = "imageAction", required = false, defaultValue = "keep") String imageAction,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        Long userId = Long.parseLong(authentication.getName());
        UpdateGroupRequest request = new UpdateGroupRequest(groupName, groupDescription);
        GroupResponse response = groupService.updateGroup(userId, groupSeq, request, imageAction, image);

        log.info("그룹 정보 수정 API 호출: userId={}, groupSeq={}, imageAction={}", userId, groupSeq, imageAction);
        return ResponseEntity.ok(response);
    }

    // ============== 초대 링크 관련 API ==============

    @Operation(summary = "초대 링크 생성", description = "그룹 초대 링크를 생성합니다. 방장만 생성 가능하며, 정원이 가득 차면 생성할 수 없습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "초대 링크 생성 성공"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
            @ApiResponse(responseCode = "409", description = "그룹 정원이 가득 참")
    })
    @PostMapping("/{groupSeq}/invitations")
    public ResponseEntity<InvitationResponse> createInvitation(
            Authentication authentication,
            @PathVariable Long groupSeq) {

        Long userId = Long.parseLong(authentication.getName());
        InvitationResponse response = groupService.createInvitation(userId, groupSeq);

        log.info("초대 링크 생성 API 호출: userId={}, groupSeq={}", userId, groupSeq);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "초대 링크로 그룹 정보 미리보기", description = "초대 링크 토큰으로 그룹 정보를 조회합니다. 참여 전 그룹 정보와 필요한 권한을 확인할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "그룹 정보 조회 성공"),
            @ApiResponse(responseCode = "404", description = "초대 링크를 찾을 수 없음")
    })
    @GetMapping("/invitations/{token}/preview")
    public ResponseEntity<InvitationPreviewResponse> getInvitationPreview(
            @PathVariable String token) {

        log.info("초대 링크 미리보기 API 호출: token={}", token);
        InvitationPreviewResponse response = groupService.getInvitationPreview(token);
        return ResponseEntity.ok(response);
    }

// ============== 그룹 참가 신청 API ==============

    @Operation(summary = "초대 링크로 그룹 참가 신청", description = "초대 링크를 통해 그룹 참가를 신청합니다. 건강 정보 공유 동의가 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참가 신청 완료"),
            @ApiResponse(responseCode = "400", description = "권한 동의 정보가 유효하지 않음"),
            @ApiResponse(responseCode = "403", description = "이미 그룹 멤버임"),
            @ApiResponse(responseCode = "409", description = "그룹 정원 초과 또는 중복 신청")
    })
    @PostMapping("/invitations/{token}/join")
    public ResponseEntity<Void> joinByInvitation(
            Authentication authentication,
            @PathVariable String token,
            @RequestBody PermissionAgreementDto agreement) {

        Long userId = Long.parseLong(authentication.getName());
        groupService.joinByInvitation(userId, token, agreement);

        log.info("초대 링크로 참가 신청 API 호출: userId={}, token={}", userId, token);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "검색으로 그룹 참가 신청", description = "그룹 검색 후 참가를 신청합니다. 건강 정보 공유 동의가 필요합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "참가 신청 완료"),
            @ApiResponse(responseCode = "400", description = "권한 동의 정보가 유효하지 않음"),
            @ApiResponse(responseCode = "403", description = "이미 그룹 멤버임"),
            @ApiResponse(responseCode = "409", description = "그룹 정원 초과 또는 중복 신청")
    })
    @PostMapping("/{groupSeq}/join")
    public ResponseEntity<Void> joinBySearch(
            Authentication authentication,
            @PathVariable Long groupSeq,
            @RequestBody PermissionAgreementDto agreement) {

        Long userId = Long.parseLong(authentication.getName());
        groupService.joinBySearch(userId, groupSeq, agreement);

        log.info("검색으로 참가 신청 API 호출: userId={}, groupSeq={}", userId, groupSeq);
        return ResponseEntity.ok().build();
    }

// ============== 방장 전용: 참가 신청 관리 API ==============

    @Operation(summary = "대기 중인 참가 신청 목록 조회", description = "방장이 대기 중인 참가 신청 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음")
    })
    @GetMapping("/{groupSeq}/join-requests")
    public ResponseEntity<List<JoinRequestResponse>> getPendingJoinRequests(
            Authentication authentication,
            @PathVariable Long groupSeq) {

        Long userId = Long.parseLong(authentication.getName());
        List<JoinRequestResponse> response = groupService.getPendingJoinRequests(userId, groupSeq);

        log.info("대기 중인 참가 신청 목록 조회 API 호출: userId={}, groupSeq={}", userId, groupSeq);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "참가 신청 승인", description = "방장이 참가 신청을 승인합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "승인 완료"),
            @ApiResponse(responseCode = "400", description = "이미 처리된 신청"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음"),
            @ApiResponse(responseCode = "409", description = "그룹 정원 초과")
    })
    @PostMapping("/join-requests/{requestSeq}/approve")
    public ResponseEntity<Void> approveJoinRequest(
            Authentication authentication,
            @PathVariable Long requestSeq) {

        Long userId = Long.parseLong(authentication.getName());
        groupService.approveJoinRequest(userId, requestSeq);

        log.info("참가 신청 승인 API 호출: userId={}, requestSeq={}", userId, requestSeq);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "참가 신청 거절", description = "방장이 참가 신청을 거절합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "거절 완료"),
            @ApiResponse(responseCode = "400", description = "이미 처리된 신청"),
            @ApiResponse(responseCode = "403", description = "방장 권한 없음")
    })
    @PostMapping("/join-requests/{requestSeq}/reject")
    public ResponseEntity<Void> rejectJoinRequest(
            Authentication authentication,
            @PathVariable Long requestSeq) {

        Long userId = Long.parseLong(authentication.getName());
        groupService.rejectJoinRequest(userId, requestSeq);

        log.info("참가 신청 거절 API 호출: userId={}, requestSeq={}", userId, requestSeq);
        return ResponseEntity.ok().build();
    }


    // ============== 그룹 건강 데이터 API ==============

    @Operation(summary = "(LocalDate) 기간별 그룹원 총 칼로리, 총 걸음 수, 총 운동 시간 조회",
            description = "지정된 기간의 그룹원의 총 칼로리, 총 걸음 수, 총 운동 시간 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @GetMapping("/{groupSeq}/total-acitivty/stats")
    public ResponseEntity<TotalActivityStatisticsResponse> getGroupTotalActivityStatistics(
            @Parameter(description = "그룹 시퀀스", required = true) @PathVariable Long groupSeq,
            @Parameter(description = "시작 날짜 (LocalDate)") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "종료 날짜 (LocalDate)") @RequestParam(required = false) LocalDate endDate
    ) {
        log.info("API 호출 - (LocalDate) 기간별 그룹원 총 칼로리, 걸음 수, 운동 시간 조회: groupSeq={}, startDate={}, endDate={}",
                groupSeq, startDate, endDate);

        TotalActivityStatisticsResponse response = groupService.getGroupTotalActivityStatistics(groupSeq, startDate, endDate);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-weekly-stats")
    public ResponseEntity<String> generateWeeklyStats() {
        weeklyStatsService.generateAllGroupWeeklyStats();
        return ResponseEntity.ok("주간 통계 생성 완료");
    }
}
