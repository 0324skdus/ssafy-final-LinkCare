package com.a307.linkcare.feature.commongroup.domain

import androidx.annotation.DrawableRes

data class Member(
    val name: String,
    val progresses: Int,
    val goal: Int,
    @DrawableRes val avatarRes: Int,
    val bubbleText: String = "",
    val isLeader: Boolean = false,
    val userPk: Long = 0L
) {
    val percent: Float get() = (progresses.coerceAtMost(goal)).toFloat() / goal.toFloat()
}