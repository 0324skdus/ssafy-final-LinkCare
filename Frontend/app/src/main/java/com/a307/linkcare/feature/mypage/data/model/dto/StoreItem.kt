package com.a307.linkcare.feature.mypage.data.model.dto

data class StoreItem(
    val id: Long,
    val imageUrl: String,
    val animatedImageUrl: String? = null,
    val price: Int,
    val owned: Boolean
)