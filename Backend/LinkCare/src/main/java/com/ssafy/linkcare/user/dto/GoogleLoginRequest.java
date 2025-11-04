package com.ssafy.linkcare.user.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
    @NotBlank(message = "ID Token은 필수입니다")
    String idToken
) {}
