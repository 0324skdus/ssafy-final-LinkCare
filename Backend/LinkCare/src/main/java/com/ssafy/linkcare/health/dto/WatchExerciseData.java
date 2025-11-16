package com.ssafy.linkcare.health.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 워치에서 받는 운동 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WatchExerciseData {

    private Long sessionId;         // 워치 고유 ID (필수) - Long으로 변경
    private Integer avgHeartRate;   // 평균 심박수
    private Float calories;         // 칼로리 - Float로 변경
    private Float distance;         // 거리 (m) - Float로 변경
    private Long durationSec;       // 운동 시간 (초) - Long으로 변경
    private Long startTimestamp;    // 시작 시간 (필수)
    private Long endTimestamp;      // 종료 시간 (필수)
}
