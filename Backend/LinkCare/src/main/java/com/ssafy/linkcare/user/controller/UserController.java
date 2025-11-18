package com.ssafy.linkcare.user.controller;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.user.dto.MyPageResponseDto;
import com.ssafy.linkcare.user.dto.ProfileUpdateRequest;
import com.ssafy.linkcare.user.dto.UpdateFcmTokenRequest;
import com.ssafy.linkcare.user.dto.UserProfileResponse;
import com.ssafy.linkcare.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "초기 설정 완료", description = "사용자의 초기 설정(기본 캐릭터 선택, 펫 이름 설정)을 완료합니다. 쿼리 파라미터로 characterId와 petName을 전달합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "초기 설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 오류)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자 또는 캐릭터를 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 초기 설정이 완료됨")
    })
    @PostMapping("/initial-setup")
    public ResponseEntity<Void> completeInitialSetup(
            Authentication authentication,
            @RequestParam("characterId") Long characterId,
            @RequestParam("petName") String petName) {
        Long userPk = Long.parseLong(authentication.getName());
        userService.completeInitialSetup(userPk, characterId, petName);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "마이페이지 정보 조회", description = "현재 로그인한 사용자의 마이페이지 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "마이페이지 정보 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/mypage")
    public ResponseEntity<MyPageResponseDto> getMyPageInfo(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        MyPageResponseDto myPageInfo = userService.getMyPageInfo(userId);

        return ResponseEntity.ok(myPageInfo);
    }

    @Operation(summary = "내 프로필 조회",
        description = "현재 로그인한 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        UserProfileResponse profile = userService.getUserProfile(userId);

        log.info("프로필 조회: userId={}", userId);
        return ResponseEntity.ok(profile);
    }

    @Operation(summary = "프로필 업데이트",
        description = "사용자의 프로필 정보(생년월일, 키, 몸무게, 성별, 운동 시작 년도, 펫 이름)를 업데이트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "프로필 업데이트 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 항목 누락 또는 유효성 검증 실패)"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(
        Authentication authentication,
        @RequestBody @jakarta.validation.Valid ProfileUpdateRequest request) {

        String userId = authentication.getName();

        if (request.birth().isAfter(LocalDate.now())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "생년월일은 미래일 수 없습니다");
        }

        userService.updateProfile(Long.parseLong(userId), request);

        return ResponseEntity.ok("프로필 업데이트 성공");
    }

    @Operation(summary = "FCM 토큰 업데이트", description = "안드로이드 앱에서 FCM 토큰을 서버에 저장합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "FCM 토큰 저장 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            Authentication authentication,
            @RequestBody UpdateFcmTokenRequest request) {

        Long userId = Long.parseLong(authentication.getName());
        userService.updateFcmToken(userId, request.fcmToken());

        log.info("FCM 토큰 업데이트 API 호출: userId={}", userId);
        return ResponseEntity.ok().build();
    }
}