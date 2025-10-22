package com.ssafy.linkcare.user.controller;

import com.ssafy.linkcare.user.dto.LoginRequest;
import com.ssafy.linkcare.user.dto.LoginResponse;
import com.ssafy.linkcare.user.dto.SignupRequest;
import com.ssafy.linkcare.user.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

}
