package com.ssafy.linkcare.user.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProfileUpdateRequest(

    @NotNull(message = "생년월일은 필수입니다")
    LocalDate birth,

    Float height,

    Float weight,

    String gender,

    Integer exerciseStartYear
) {}
