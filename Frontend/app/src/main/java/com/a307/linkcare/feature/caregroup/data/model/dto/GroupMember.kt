package com.a307.linkcare.feature.caregroup.data.model.dto

import androidx.annotation.DrawableRes

data class GroupMember(
    val id: Long,
    val name: String,
    @DrawableRes val avatarRes: Int?,
    val isLeader: Boolean,
    val avatarUrl: String? = null
)