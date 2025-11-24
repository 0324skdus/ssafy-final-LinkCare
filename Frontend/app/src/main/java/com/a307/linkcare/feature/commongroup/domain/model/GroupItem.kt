package com.a307.linkcare.feature.commongroup.domain.model

import androidx.annotation.DrawableRes

data class GroupItem(
    val id: Long,
    val title: String,
    val summary: String,
    val pace: String,       // ex) "30kcal | 30min | 4000steps | 3km"
    val members: String,    // ex) "3/6"
    @DrawableRes val thumbnailRes: Int? = null
)