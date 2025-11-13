package com.ssafy.linkcare.user.controller;

import com.ssafy.linkcare.exception.CustomException;
import com.ssafy.linkcare.exception.ErrorCode;
import com.ssafy.linkcare.user.dto.ProfileUpdateRequest;
import com.ssafy.linkcare.user.dto.UpdateFcmTokenRequest;
import com.ssafy.linkcare.user.dto.UserProfileResponse;
import com.ssafy.linkcare.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
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

    /*
     * 내 프로필 조회
     * GET /api/users/me
     */
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

    /*
     * 프로필 업데이트
        * PUT /api/users/profile
    */
    @Operation(summary = "프로필 업데이트",
        description = "사용자의 프로필 정보(생년월일, 키, 몸무게, 성별, 운동 시작 년도)를 업데이트합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "프로필 업데이트 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 항목 누락)"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> updateProfile(
        Authentication authentication,
        @RequestParam("birth") String birth,
        @RequestParam("height") Float height,
        @RequestParam("weight") Float weight,
        @RequestParam("gender") String gender,
        @RequestParam("exerciseStartYear") Integer exerciseStartYear) {

        String userId = authentication.getName();

        // 검증
        LocalDate birthDate = validateAndParseBirth(birth);
        validateHeight(height);
        validateWeight(weight);
        validateGender(gender);
        validateExerciseStartYear(exerciseStartYear);

        // 1. ProfileUpdateRequest 생성
        ProfileUpdateRequest request = new ProfileUpdateRequest(
            birthDate,
            height,
            weight,
            gender,
            exerciseStartYear
        );

        // 2. 프로필 정보 + 이미지 URL 업데이트
        userService.updateProfile(Long.parseLong(userId), request);

        return ResponseEntity.ok("프로필 업데이트 성공");
    }

    // 검증 메서드들
    private LocalDate validateAndParseBirth(String birth) {
        try {
            LocalDate birthDate = LocalDate.parse(birth);
            if (birthDate.isAfter(LocalDate.now())) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "생년월일은 미래일 수 없습니다");
            }
            return birthDate;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "생년월일 형식이 올바르지 않습니다");
        }
    }

    private void validateHeight(Float height) {
        if (height != null && (height <= 0 || height > 300)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "키는 0~300 사이여야 합니다");
        }
    }

    private void validateWeight(Float weight) {
        if (weight != null && (weight <= 0 || weight > 500)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "몸무게는 0~500 사이여야 합니다");
        }
    }

    private void validateGender(String gender) {
        if (gender != null && !gender.trim().matches("남|여")) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "성별은 남, 여 중 하나여야 합니다");
        }
    }

    private void validateExerciseStartYear(Integer year) {
        int currentYear = LocalDate.now().getYear();
        if (year != null && (year < 1900 || year > currentYear)) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "운동 시작 년도가 올바르지 않습니다");
        }
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