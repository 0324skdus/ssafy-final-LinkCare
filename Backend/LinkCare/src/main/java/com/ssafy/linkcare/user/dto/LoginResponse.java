package com.ssafy.linkcare.user.dto;

public record LoginResponse(
    String accessToken,
    String refreshToken,
    Long userPk,
    String email,
    String name,
    Boolean needsProfileCompletion
) {}