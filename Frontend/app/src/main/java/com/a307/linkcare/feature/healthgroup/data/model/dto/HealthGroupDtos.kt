package com.a307.linkcare.feature.healthgroup.data.model.dto

import androidx.annotation.DrawableRes
import com.a307.linkcare.feature.caregroup.data.model.dto.GroupMember
import com.a307.linkcare.feature.healthgroup.data.model.request.ShareOptions

data class HealthGroup(
    val id: Long,
    val name: String,
    val description: String,
    val maxMember: Int,
    val share: ShareOptions,
    val inviteLink: String,
    val members: List<GroupMember>,
    @DrawableRes val avatarRes: Int? = null,
    val baseline: HealthBaseline? = null
)

data class HealthBaseline(
    val kcal: Int? = null,
    val minutes: Int? = null,
    val steps: Int? = null,
    val km: Float? = null
)
