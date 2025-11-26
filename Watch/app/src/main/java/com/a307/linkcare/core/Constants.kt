package com.a307.linkcare.core

/**
 * 애플리케이션 전역 상수 정의
 */
object Constants {

    /**
     * 운동 데이터 전송 및 타이밍 관련 상수
     */
    object Exercise {
        /** 실시간 메트릭 전송 주기 (밀리초) */
        const val METRICS_SEND_INTERVAL_MS = 2000L

        /** 운동 종료 후 마지막 업데이트 대기 시간 (밀리초) */
        const val END_EXERCISE_DELAY_MS = 500L
    }

    /**
     * UI 관련 상수
     */
    object UI {
        /** 캐릭터 기본 스케일 배율 */
        const val CHARACTER_SCALE = 2.0f

        /** 캐릭터 하단 패딩 (dp) */
        const val CHARACTER_BOTTOM_PADDING_DP = 50

        /** 캐릭터 애니메이션 프레임 간격 (밀리초) */
        const val ANIMATION_FRAME_DELAY_MS = 150L
    }

    /**
     * 기본 ID 값
     */
    object Defaults {
        /** 기본 캐릭터 ID */
        const val DEFAULT_CHARACTER_ID = 1

        /** 기본 배경 ID */
        const val DEFAULT_BACKGROUND_ID = 1
    }
}
