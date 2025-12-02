package com.ssafy.linkcare.user.dto;

import java.time.LocalDate;

public record UserProfileResponse(
    String name,
    LocalDate birth,
    Float height,
    Float weight,
    String gender,
    Integer exerciseStartYear,
    String petName
) {}
