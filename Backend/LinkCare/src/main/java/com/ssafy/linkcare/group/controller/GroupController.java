package com.ssafy.linkcare.group.controller;

import com.ssafy.linkcare.group.dto.*;
import com.ssafy.linkcare.group.enums.GroupType;
import com.ssafy.linkcare.group.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
@Tag(name = "그룹", description = "헬스/케어 그룹 관련 API")
public class GroupController {

    private final GroupService groupService;

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
}
