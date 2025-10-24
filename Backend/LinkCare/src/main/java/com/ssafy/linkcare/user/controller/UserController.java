package com.ssafy.linkcare.user.controller;

import com.ssafy.linkcare.user.dto.ProfileUpdateRequest;
import com.ssafy.linkcare.user.service.UserService;
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
    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(Authentication authentication, @Valid @RequestBody ProfileUpdateRequest request) {

        // JWT에서 userId 추출
        String userId = authentication.getName();

        userService.updateProfile(Long.parseLong(userId), request);
        return ResponseEntity.ok("프로필 업데이트 성공");
    }
}