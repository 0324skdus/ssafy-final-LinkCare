package com.ssafy.linkcare.group.dto;

/*
    * 건강 데이터 공유 권한 DTO
        * - 필수 항목: 걸음수, 심박수, 운동 (항상 true)
        * - 선택 항목: 수면, 음수량, 혈압, 혈당
*/
public record HealthPermissionDto(
        // 선택 항목만 받음 (필수 항목은 자동 true)
        Boolean isSleepAllowed,           // 수면 데이터 공유
        Boolean isWaterIntakeAllowed,     // 음수량 공유
        Boolean isBloodPressureAllowed,   // 혈압 공유
        Boolean isBloodSugarAllowed       // 혈당 공유
) {
    // 기본값 생성자 (모두 false)
    public HealthPermissionDto() {
        this(false, false, false, false);
    }
}
