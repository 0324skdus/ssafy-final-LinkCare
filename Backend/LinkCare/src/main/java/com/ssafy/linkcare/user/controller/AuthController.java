package com.ssafy.linkcare.user.controller;

import com.ssafy.linkcare.user.dto.*;
import com.ssafy.linkcare.user.service.AuthService;
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
public class AuthController {

    private final AuthService authService;
    private static final String BEARER_PREFIX = "Bearer ";

    /*
        * 이메일 중복 확인
        * GET /api/auth/check-email?email=test@example.com
    */
    @GetMapping("/check-email")
    public ResponseEntity<Boolean> checkEmailDuplicate(@RequestParam String email) {
        boolean isDuplicate = authService.checkEmailDuplicate(email);
        return ResponseEntity.ok(isDuplicate);
    }

    /*
        * 이메일 인증 코드 발송
        * POST /api/auth/send-verification-code
    */
    @PostMapping("/send-verification-code")
    public ResponseEntity<String> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request) {
        authService.sendVerificationCode(request.email());
        return ResponseEntity.ok("인증 코드가 발송되었습니다");
    }

    /*
        * 인증 코드 검증
        * POST /api/auth/verify-code
    */
    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        authService.verifyCode(request.email(), request.code());
        return ResponseEntity.ok("인증이 완료되었습니다");
    }

    /*
        * 회원가입
        * POST /api/auth/signup
    */
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        authService.signup(request);
        return ResponseEntity.ok("회원가입이 완료되었습니다");
    }

    /*
        * 로그인
        * POST /api/auth/login
    */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    // Token 재발급
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@RequestHeader("Authorization") String authorizationHeader) {
        // "Bearer " 제거
        String refreshToken = extractToken(authorizationHeader);
        LoginResponse response = authService.refreshAccessToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /// 로그아웃
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
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        LoginResponse response = authService.googleLogin(request.idToken());
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
