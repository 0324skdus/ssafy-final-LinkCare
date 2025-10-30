package com.ssafy.linkcare.user.controller;

import com.ssafy.linkcare.s3.S3Service;
import com.ssafy.linkcare.user.dto.ProfileUpdateRequest;
import com.ssafy.linkcare.user.entity.User;
import com.ssafy.linkcare.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final S3Service s3Service;

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
        @RequestParam("exerciseStartYear") Integer exerciseStartYear,
        @RequestPart(value = "image", required = false) MultipartFile image) {

        String userId = authentication.getName();

        // 1. ProfileUpdateRequest 생성
        ProfileUpdateRequest request = new ProfileUpdateRequest(
            LocalDate.parse(birth),
            height,
            weight,
            gender,
            exerciseStartYear
        );

        // 2. 이미지가 있으면 S3 업로드
        String imageUrl = null;
        if (image != null && !image.isEmpty()) {
            // 기존 이미지 삭제 (기본 이미지 제외)
            User user = userService.getUserById(Long.parseLong(userId));
            if (user.getImageUrl() != null &&
                !user.getImageUrl().contains("default_profile.png")) {
                s3Service.deleteFile(user.getImageUrl());
            }

            // 새 이미지 업로드
            imageUrl = s3Service.uploadFile(image);
        }

        // 3. 프로필 정보 + 이미지 URL 업데이트
        userService.updateProfile(Long.parseLong(userId), request, imageUrl);

        return ResponseEntity.ok("프로필 업데이트 성공");
    }
}