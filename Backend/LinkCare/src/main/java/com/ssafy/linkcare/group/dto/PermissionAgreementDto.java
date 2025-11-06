package com.ssafy.linkcare.group.dto;

/*
    * 그룹 참가 신청 시 사용자가 동의한 선택 권한 정보
    * 필수 권한(걸음수, 심박수, 운동)은 자동으로 true이므로 포함하지 않음
*/
public record PermissionAgreementDto(
        Boolean isSleepAllowed,          // 수면 데이터 공유 동의
        Boolean isWaterIntakeAllowed,    // 물 섭취량 공유 동의
        Boolean isBloodPressureAllowed,  // 혈압 데이터 공유 동의
        Boolean isBloodSugarAllowed      // 혈당 데이터 공유 동의
) {
    /*
       * 기본값 생성자 (모두 false)
    */
    public PermissionAgreementDto() {
        this(false, false, false, false);
    }

    /*
        * null 값을 false로 변환
    */
    public PermissionAgreementDto {
        isSleepAllowed = isSleepAllowed != null ? isSleepAllowed : false;
        isWaterIntakeAllowed = isWaterIntakeAllowed != null ? isWaterIntakeAllowed : false;
        isBloodPressureAllowed = isBloodPressureAllowed != null ? isBloodPressureAllowed : false;
        isBloodSugarAllowed = isBloodSugarAllowed != null ? isBloodSugarAllowed : false;
    }
}
