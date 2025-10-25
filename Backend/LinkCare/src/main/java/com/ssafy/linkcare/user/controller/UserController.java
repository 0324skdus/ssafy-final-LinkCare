package com.ssafy.linkcare.user.controller;

import com.ssafy.linkcare.user.dto.ProfileUpdateRequest;
import com.ssafy.linkcare.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(Authentication authentication, @Valid @RequestBody ProfileUpdateRequest request) {

        // JWT에서 userId 추출
        String userId = authentication.getName();

        userService.updateProfile(Long.parseLong(userId), request);
        return ResponseEntity.ok("프로필 업데이트 성공");
    }
}