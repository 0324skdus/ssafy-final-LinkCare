package com.ssafy.linkcare.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record ProfileUpdateRequest(

    @NotNull(message = "생년월일은 필수입니다")
    @JsonFormat(pattern = "yyyy-MM-dd")
    LocalDate birth,

    @Min(value = 1, message = "키는 1~300 사이여야 합니다")
    @Max(value = 300, message = "키는 1~300 사이여야 합니다")
    Float height,

    @Min(value = 1, message = "몸무게는 1~500 사이여야 합니다")
    @Max(value = 500, message = "몸무게는 1~500 사이여야 합니다")
    Float weight,

    @Pattern(regexp = "남|여", message = "성별은 남, 여 중 하나여야 합니다")
    String gender,

    @Min(value = 1900, message = "운동 시작 년도가 올바르지 않습니다")
    @Max(value = 2100, message = "운동 시작 년도가 올바르지 않습니다")
    Integer exerciseStartYear,

    String petName
) {}
