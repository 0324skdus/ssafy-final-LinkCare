package com.a307.linkcare.feature.healthgroup.domain

import com.a307.linkcare.feature.commongroup.domain.Member

/**
 * HealthGroupHome 화면에서 멤버 정보와 일일 활동 정보를 함께 다루기 위한 데이터 클래스
 *
 * @param memberInfo 기존 멤버 정보 (이름, 목표치 등)
 * @param activity API를 통해 가져온 실제 활동 정보 (걸음수, 칼로리 등)
 */
data class MemberWithActivity(
    val memberInfo: Member,
    val activity: ActualActivity
)
