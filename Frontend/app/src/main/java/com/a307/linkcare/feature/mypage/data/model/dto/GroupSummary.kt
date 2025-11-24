package com.a307.linkcare.feature.mypage.data.model.dto

import androidx.annotation.DrawableRes
import com.a307.linkcare.feature.mypage.ui.mygroups.GroupType

data class GroupSummary(
    val id: Long,
    val name: String,
    val type: GroupType,
    val minGoalText: String,
    val desc: String,
    val current: Int,
    val max: Int,
    @DrawableRes val imageRes: Int = 0,
    val imageUrl: String? = null,
    val joinStatus: String = "MEMBER",
    val isPending: Boolean = false
)