package com.ssafy.linkcare.group.controller;

import com.ssafy.linkcare.ai.service.GoalGenerationService;
import com.ssafy.linkcare.gpt.dto.WeeklyHeaderResponse;
import com.ssafy.linkcare.group.dto.*;
import com.ssafy.linkcare.group.entity.WeeklyGroupGoals;
import com.ssafy.linkcare.group.enums.GroupType;
import com.ssafy.linkcare.group.scheduler.GroupHeaderScheduler;
import com.ssafy.linkcare.group.service.GroupService;
import com.ssafy.linkcare.group.service.WeeklyStatsService;
import com.ssafy.linkcare.health.dto.TotalActivityStatisticsResponse;
import com.ssafy.linkcare.health.dto.WaterIntakeStatisticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
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
    private final GoalGenerationService goalGenerationService;
    private final GroupHeaderScheduler groupHeaderScheduler;

    @Operation(summary = "모든 그룹 목록 조회", description = "모든 그룹 목록을 조회합니다. type 파라미터로 HEALTH/CARE 필터링 가능")
    @GetMapping
    public ResponseEntity<List<GroupResponse>> getAllGroups(
            Authentication authentication,
            @RequestParam(required = false) GroupType type) {

        Long userId = authentication != null ? Long.parseLong(authentication.getName()) : null;

        List<GroupResponse> response = groupService.getAllGroups(userId, type);
        return ResponseEntity.ok(response);
    }

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
    // 내가 신청한 그룹 목록 조회
    @GetMapping("/my-pending")
    @Operation(summary = "내가 신청한 그룹 목록 조회", description = "승인 대기 중인 내 신청 목록을 조회합니다. type 파라미터로 HEALTH/CARE 필터링 가능")
    public ResponseEntity<List<GroupResponse>> getMyPendingGroups(
            Authentication authentication,
            @RequestParam(required = false) GroupType type) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("내 신청 그룹 목록 조회 요청: userId={}, type={}", userId, type);

        List<GroupResponse> response = groupService.getMyPendingGroups(userId, type);
        return ResponseEntity.ok(response);
    }

    // 그룹 검색
    @GetMapping("/search")
    @Operation(summary = "그룹 검색", description = "그룹 이름으로 검색합니다")
    public ResponseEntity<List<GroupResponse>> searchGroups(
            Authentication authentication,
            @RequestParam String keyword) {

        Long userId = authentication != null ? Long.parseLong(authentication.getName()) : null;
        log.info("그룹 검색 요청: keyword={}, userId={}", keyword, userId);
        List<GroupResponse> response = groupService.searchGroups(keyword, userId);
        return ResponseEntity.ok(response);
    }

    // 그룹 상세 조회
    @GetMapping("/{groupSeq}")
    @Operation(summary = "그룹 상세 조회", description = "그룹의 상세 정보와 멤버 목록을 조회합니다")
    public ResponseEntity<GroupDetailResponse> getGroupDetail(
            Authentication authentication,
            @PathVariable Long groupSeq) {

        Long userId = Long.parseLong(authentication.getName());
        log.info("그룹 상세 조회 요청: groupSeq={}, userId={}", groupSeq, userId);
        GroupDetailResponse response = groupService.getGroupDetail(groupSeq, userId);
        return ResponseEntity.ok(response);
    }

    // 그룹 정보 수정
    @Operation(summary = "그룹 정보 수정", description = "그룹 이름, 소개, 이미지를 수정합니다. 케어 그룹인 경우 선택 권한 항목도 수정 가능합니다. 방장만 수정 가능합니다.")
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
            @RequestPart(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "isSleepRequired", required = false) Boolean isSleepRequired,
            @RequestParam(value = "isWaterIntakeRequired", required = false) Boolean isWaterIntakeRequired,
            @RequestParam(value = "isBloodPressureRequired", required = false) Boolean isBloodPressureRequired,
            @RequestParam(value = "isBloodSugarRequired", required = false) Boolean isBloodSugarRequired,
            @RequestParam(value = "minCalorie", required = false) Float minCalorie,
            @RequestParam(value = "minStep", required = false) Integer minStep,
            @RequestParam(value = "minDistance", required = false) Float minDistance,
            @RequestParam(value = "minDuration", required = false) Integer minDuration) {

        Long userId = Long.parseLong(authentication.getName());

        // 목표 기준 DTO 생성 (헬스 그룹 전용)
        GoalCriteriaDto goalCriteria = null;
        if (minCalorie != null || minStep != null || minDistance != null || minDuration != null) {
            goalCriteria = new GoalCriteriaDto(minCalorie, minStep, minDistance, minDuration);
        }

        UpdateGroupRequest request = new UpdateGroupRequest(
                groupName,
                groupDescription,
                isSleepRequired,
                isWaterIntakeRequired,
                isBloodPressureRequired,
                isBloodSugarRequired,
                goalCriteria
        );
        GroupResponse response = groupService.updateGroup(userId, groupSeq, request, imageAction, image);

        log.info("그룹 정보 수정 API 호출: userId={}, groupSeq={}, imageAction={}", userId, groupSeq, imageAction);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "그룹 상세 정보 조회 (멤버 프로필 포함)", description = "그룹 ID로 그룹의 기본 정보와 멤버들의 프로필(캐릭터, 배경 이미지)을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @GetMapping("/{groupSeq}/details")
    public ResponseEntity<MyGroupDetailResponseDto> getGroupDetails(
            Authentication authentication, @PathVariable Long groupSeq) {
        Long userId = Long.valueOf(authentication.getName());
        MyGroupDetailResponseDto response = groupService.getGroupDetails(userId, groupSeq);

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

    @Operation(summary = "(LocalDate) 기간별 그룹원 수면 시간, 총 수면시간, 평균 수면시간 조회",
            description = "지정된 기간의 그룹원의 총 칼로리, 총 걸음 수, 총 운동 시간 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @GetMapping("/{groupSeq}/sleep-statistics")
    public ResponseEntity<GroupSleepStatisticsResponse> getGroupSleepStatistics(
            @PathVariable Long groupSeq,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        GroupSleepStatisticsResponse response = groupService.getGroupSleepStatistics(groupSeq, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "그룹 걸음수 통계 조회",
            description = "오늘 날짜 기준 그룹원들의 개별 걸음수와 그룹 전체 총 걸음수를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GroupStepStatisticsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "그룹을 찾을 수 없음"
            )
    })
    @GetMapping("/{groupSeq}/step-statistics")
    public ResponseEntity<GroupStepStatisticsResponse> getGroupStepStatistics(
            @Parameter(description = "그룹 시퀀스", required = true, example = "1")
            @PathVariable Long groupSeq) {

        GroupStepStatisticsResponse response = groupService.getGroupStepStatistics(groupSeq);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "특정 날짜 그룹 걸음수 통계 조회",
            description = "특정 날짜 기준 그룹원들의 개별 걸음수와 그룹 전체 총 걸음수를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GroupStepStatisticsResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "그룹을 찾을 수 없음"
            )
    })

    @GetMapping("/{groupSeq}/step-statistics/{date}")
    public ResponseEntity<GroupStepStatisticsResponse> getGroupStepStatisticsByDate(
            @Parameter(description = "그룹 시퀀스", required = true, example = "1")
            @PathVariable Long groupSeq,
            @PathVariable LocalDate date) {

        GroupStepStatisticsResponse response = groupService.getGroupStepStatisticsByDate(groupSeq, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{groupSeq}/step-statistics/period")
    public ResponseEntity<GroupStepStatisticsResponse> getGroupStepStatisticsByPeriod(
            @Parameter(description = "그룹 시퀀스", required = true, example = "1")
            @PathVariable Long groupSeq,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {

        GroupStepStatisticsResponse response = groupService.getGroupStepStatisticsByPeriod(groupSeq, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/generate-weekly-stats")
    public ResponseEntity<String> generateWeeklyStats() {
        weeklyStatsService.generateAllGroupWeeklyStats();
        return ResponseEntity.ok("주간 통계 생성 완료");
    }

    @Operation(summary = "현재 주차 목표 조회",
            description = "이번 주(월요일 기준) 목표를 조회합니다. " +
                    "목표가 없으면 404를 반환하며, 프론트엔드에서 POST 목표 생성 API를 호출해야 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "목표 조회 성공"),
            @ApiResponse(responseCode = "404", description = "목표를 찾을 수 없음 또는 그룹을 찾을 수 없음")
    })
    @GetMapping("/{groupSeq}/goals/current")
    public ResponseEntity<WeeklyGroupGoalResponse> getCurrentGoal(
            @PathVariable Long groupSeq) {

        log.info("현재 주차 목표 조회 API 호출: groupSeq={}", groupSeq);

        WeeklyGroupGoals goal = goalGenerationService.getCurrentWeekGoal(groupSeq);
        WeeklyGroupGoalResponse response = WeeklyGroupGoalResponse.from(goal);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "AI 기반 그룹 목표 생성",
            description = "저번 주부터 최대 3주 데이터를 기반으로 AI가 예측한 주간 목표를 생성합니다. " +
                    "requestDate는 어떤 요일이든 가능하며, 해당 날짜가 속한 주(월~일)의 목표가 생성됩니다. " +
                    "목표는 주간 전체 값으로 저장되며, 남은 일수 계산은 프론트엔드에서 처리합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "목표 생성 성공"),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "과거 통계 데이터가 존재하지 않음")
    })
    @PostMapping("/{groupSeq}/goals")
    public ResponseEntity<WeeklyGroupGoalResponse> generateGroupGoal(
            @PathVariable Long groupSeq,
            @RequestParam LocalDate requestDate) {

        log.info("그룹 목표 생성 API 호출: groupSeq={}, requestDate={}", groupSeq, requestDate);

        WeeklyGroupGoals goal = goalGenerationService.generateNextWeekGoal(groupSeq, requestDate);
        WeeklyGroupGoalResponse response = WeeklyGroupGoalResponse.from(goal);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "현재 주차 목표 수정",
            description = "사용자가 선택한 메트릭 타입과 목표값을 저장합니다. " +
                    "최소 기준보다 작은 값은 자동으로 최소값으로 조정됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "목표 수정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효하지 않은 metricType 또는 goalValue)"),
            @ApiResponse(responseCode = "404", description = "목표를 찾을 수 없음 또는 그룹을 찾을 수 없음")
    })
    @PutMapping("/{groupSeq}/goals")
    public ResponseEntity<WeeklyGroupGoalResponse> updateGroupGoal(
            @PathVariable Long groupSeq,
            @RequestBody UpdateWeeklyGroupGoalRequest request) {

        log.info("그룹 목표 수정 API 호출: groupSeq={}, metricType={}, goalValue={}",
                groupSeq, request.selectedMetricType(), request.goalValue());

        WeeklyGroupGoals goal = goalGenerationService.updateCurrentWeekGoal(groupSeq, request);
        WeeklyGroupGoalResponse response = WeeklyGroupGoalResponse.from(goal);

        return ResponseEntity.ok(response);
    }

    // ============== 그룹원 동의 관리 API ==============

    @Operation(summary = "그룹원 권한 동의", description = "그룹원이 변경된 권한 설정에 동의합니다. 케어 그룹 전용 기능입니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "동의 처리 완료"),
            @ApiResponse(responseCode = "400", description = "권한 동의 정보가 유효하지 않음"),
            @ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @PostMapping("/{groupSeq}/permissions/agree")
    public ResponseEntity<Void> agreeToGroupPermissions(
            Authentication authentication,
            @PathVariable Long groupSeq,
            @RequestBody PermissionAgreementDto agreement) {

        Long userId = Long.parseLong(authentication.getName());
        groupService.agreeToGroupPermissions(userId, groupSeq, agreement);

        log.info("그룹원 권한 동의 API 호출: userId={}, groupSeq={}", userId, groupSeq);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "케어 그룹 권한 자동 동의",
            description = "권한 변경 알림 받은 후 간편하게 동의합니다. 그룹이 요구하는 모든 권한에 자동으로 동의됩니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "자동 동의 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    @PostMapping("/{groupSeq}/permissions/auto-agree")
    public ResponseEntity<Void> autoAgreeToGroupPermissions(
            Authentication authentication,
            @PathVariable Long groupSeq) {

        Long userId = Long.parseLong(authentication.getName());
        groupService.autoAgreeToGroupPermissions(userId, groupSeq);

        log.info("그룹원 권한 자동 동의 API 호출: userId={}, groupSeq={}", userId, groupSeq);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/weekly-header/{groupSeq}")
    @Operation(summary = "그룹 주간 헤더 문구 조회", description = "저장된 이번 주 목표 문구를 제공합니다")
    public ResponseEntity<WeeklyHeaderResponse> getWeeklyHeader(
            @PathVariable Long groupSeq
    ) {
        log.info("=== 주간 헤더 조회 ===");
        log.info("그룹 ID: {}", groupSeq);

        WeeklyHeaderResponse response = groupService.getWeeklyHeader(groupSeq);

        log.info("헤더 메시지: {}", response.getHeaderMessage());
        log.info("생성 시간: {}", response.getGeneratedAt());

        return ResponseEntity.ok(response);
    }

    // 관리자용 - 수동 재생성 API (선택사항)
    @PostMapping("/weekly-header/{groupSeq}/regenerate")
    @Operation(summary = "주간 헤더 강제 재생성")
    public ResponseEntity<WeeklyHeaderResponse> regenerateWeeklyHeader(
            @PathVariable Long groupSeq
    ) {
        log.info("=== 주간 헤더 수동 재생성 ===");
        WeeklyHeaderResponse response = groupHeaderScheduler.updateGroupHeader(groupSeq);
        return ResponseEntity.ok(response);
    }

    // ============== 그룹 관리 API ==============

    @PostMapping("/{groupSeq}/delegate")
    @Operation(summary = "그룹장 위임", description = "그룹장이 다른 멤버에게 그룹장 권한을 위임합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "위임 완료"),
            @ApiResponse(responseCode = "403", description = "그룹장 권한 없음"),
            @ApiResponse(responseCode = "404", description = "그룹 또는 멤버를 찾을 수 없음")
    })
    public ResponseEntity<Void> delegateLeader(
            Authentication authentication,
            @PathVariable Long groupSeq,
            @RequestParam Long newLeaderUserSeq) {

        Long currentUserSeq = Long.parseLong(authentication.getName());
        log.info("그룹장 위임 요청: groupSeq={}, currentUser={}, newLeader={}",
                groupSeq, currentUserSeq, newLeaderUserSeq);

        groupService.delegateLeader(groupSeq, newLeaderUserSeq, currentUserSeq);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupSeq}/members/{targetUserSeq}")
    @Operation(summary = "그룹원 내보내기", description = "그룹장이 그룹원을 내보냅니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "내보내기 완료"),
            @ApiResponse(responseCode = "403", description = "그룹장 권한 없음 또는 그룹장은 내보낼 수 없음"),
            @ApiResponse(responseCode = "404", description = "그룹 또는 멤버를 찾을 수 없음")
    })
    public ResponseEntity<Void> kickMember(
            Authentication authentication,
            @PathVariable Long groupSeq,
            @PathVariable Long targetUserSeq) {

        Long currentUserSeq = Long.parseLong(authentication.getName());
        log.info("그룹원 내보내기 요청: groupSeq={}, currentUser={}, targetUser={}",
                groupSeq, currentUserSeq, targetUserSeq);

        groupService.kickMember(groupSeq, targetUserSeq, currentUserSeq);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupSeq}/leave")
    @Operation(summary = "그룹 탈퇴", description = "그룹원이 그룹을 탈퇴합니다. 그룹장은 다른 멤버가 있으면 탈퇴할 수 없습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "탈퇴 완료"),
            @ApiResponse(responseCode = "400", description = "그룹장은 다른 멤버가 있을 때 탈퇴할 수 없음"),
            @ApiResponse(responseCode = "404", description = "그룹 또는 멤버를 찾을 수 없음")
    })
    public ResponseEntity<Void> leaveGroup(
            Authentication authentication,
            @PathVariable Long groupSeq) {

        Long currentUserSeq = Long.parseLong(authentication.getName());
        log.info("그룹 탈퇴 요청: groupSeq={}, currentUser={}", groupSeq, currentUserSeq);

        groupService.leaveGroup(groupSeq, currentUserSeq);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupSeq}")
    @Operation(summary = "그룹 삭제", description = "그룹장이 그룹을 삭제합니다. 그룹장만 가능합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 완료"),
            @ApiResponse(responseCode = "403", description = "그룹장 권한 없음"),
            @ApiResponse(responseCode = "404", description = "그룹을 찾을 수 없음")
    })
    public ResponseEntity<Void> deleteGroup(
            Authentication authentication,
            @PathVariable Long groupSeq) {

        Long currentUserSeq = Long.parseLong(authentication.getName());
        log.info("그룹 삭제 요청: groupSeq={}, currentUser={}", groupSeq, currentUserSeq);

        groupService.deleteGroup(groupSeq, currentUserSeq);

        return ResponseEntity.ok().build();
    }
}
