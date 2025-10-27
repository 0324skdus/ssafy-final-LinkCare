package com.ssafy.linkcare.user.controller;

import com.ssafy.linkcare.user.dto.*;
import com.ssafy.linkcare.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "회원가입, 로그인, 로그아웃 관련 API")
public class AuthController {

    private final AuthService authService;
    private static final String BEARER_PREFIX = "Bearer ";

    /*
        * 이메일 중복 확인
        * GET /api/auth/check-email?email=test@example.com
    */
    @Operation(summary = "이메일 중복 확인", description = "회원가입 시 이메일 중복 여부를 확인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "중복 확인 완료 (true: 중복, false: 사용 가능)")
    })
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailDuplicate(@RequestParam String email) {
        boolean isDuplicate = authService.checkEmailDuplicate(email);
        return ResponseEntity.ok(isDuplicate);
    }

    /*
        * 이메일 인증 코드 발송
        * POST /api/auth/send-verification-code
    */
    @Operation(summary = "이메일 인증 코드 발송", description = "입력한 이메일로 6자리 인증 코드를 발송합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 코드 발송 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (이메일 형식 오류)")
    })
    @PostMapping("/send-verification-code")
    public ResponseEntity<String> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        authService.sendVerificationCode(request.email());
        return ResponseEntity.ok("인증 코드가 발송되었습니다");
    }

    /*
        * 인증 코드 검증
        * POST /api/auth/verify-code
    */
    @Operation(summary = "인증 코드 검증", description = "이메일로 받은 6자리 인증 코드를 검증합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "인증 성공"),
        @ApiResponse(responseCode = "400", description = "인증 코드 불일치 또는 만료")
    })
    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        authService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok("인증이 완료되었습니다");
    }

    /*
        * 회원가입
        * POST /api/auth/signup
    */
    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 이름으로 회원가입합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "회원가입 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 항목 누락, 비밀번호 형식 오류)"),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다");
    }

    /*
        * 로그인
        * POST /api/auth/login
    */
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치"),
        @ApiResponse(responseCode = "404", description = "존재하지 않는 사용자")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // Token 재발급
    @Operation(summary = "토큰 재발급", description = "Refresh Token으로 새로운 Access Token을 발급받습니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token"),
        @ApiResponse(responseCode = "404", description = "저장된 Refresh Token을 찾을 수 없음")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestHeader("Authorization") String authorizationHeader) {
        // "Bearer " 제거
        String refreshToken = extractToken(authorizationHeader);
        LoginResponse response = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    // 로그아웃
    @Operation(summary = "로그아웃", description = "로그아웃하고 Refresh Token을 삭제합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    @PostMapping("/logout")
    public ResponseEntity<String> logout(Authentication authentication) {
        // Spring이 자동으로 주입
        String userId = authentication.getName();
        authService.logout(userId);
        return ResponseEntity.ok("로그아웃이 완료되었습니다");
    }

    /*
        * Google 소셜 로그인
        * POST /api/auth/google
    */
    @Operation(summary = "Google 소셜 로그인", description = "Google ID Token으로 로그인합니다.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 ID Token"),
        @ApiResponse(responseCode = "500", description = "Google 인증 서버 오류")
    })
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        LoginResponse response = authService.googleLogin(request.idToken());
        return ResponseEntity.ok(response);
    }

    /*
        * 카카오 로그인
        * POST /api/auth/kakao
    */
    @Operation(summary = "카카오 소셜 로그인",
            description = "카카오에서 발급받은 인가 코드로 로그인/회원가입을 처리합니다")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 인가 코드"),
            @ApiResponse(responseCode = "500", description = "카카오 로그인 처리 실패")
    })
    @PostMapping("/kakao")
    public ResponseEntity<LoginResponse> kakaoLogin(@Valid @RequestBody KakaoLoginRequest request) {
        LoginResponse response = authService.kakaoLogin(request.accessToken());
        return ResponseEntity.ok(response);
    }


    /**
        * Authorization Header에서 Token 추출
        * @param authorizationHeader "Bearer {token}"
        * @return token
    */
    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_PREFIX)) {
            return authorizationHeader.substring(BEARER_PREFIX.length());
        }
        throw new IllegalArgumentException("유효하지 않은 Authorization Header");
    }
}
