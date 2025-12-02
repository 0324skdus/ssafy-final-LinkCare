package com.a307.linkcare.feature.commongroup.domain.model

import androidx.annotation.DrawableRes

data class Member(
    val name: String,
    val progresses: Int,
    val goal: Int,
    @DrawableRes val avatarRes: Int,
    val bubbleText: String = "",
    val isLeader: Boolean = false,
    val userPk: Long = 0L,
    val status: String = "Good",
    val summary: String = "활동이 안정적이에요."
) {
    val percent: Float get() = (progresses.coerceAtMost(goal)).toFloat() / goal.toFloat()
}