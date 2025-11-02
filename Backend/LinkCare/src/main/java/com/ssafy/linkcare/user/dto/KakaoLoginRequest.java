package com.ssafy.linkcare.user.dto;

import jakarta.validation.constraints.NotBlank;

public record KakaoLoginRequest(
        @NotBlank(message = "Access Token은 필수입니다")
        String accessToken
) {}