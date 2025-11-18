package com.a307.linkcare.feature.mypage.domain

data class ShopCharacterDto(
    val characterId: Long,
    val name: String,
    val description: String,
    val baseImageUrl: String,
    val animatedImageUrl: String,
    val price: Int,
    val unlocked: Boolean
)

data class ShopCharacterResponse(
    val userPoints: Int,
    val characters: List<ShopCharacterDto>
)

data class ShopBackgroundDto(
    val backgroundId: Long,
    val name: String,
    val description: String,
    val imageUrl: String,
    val price: Int,
    val unlocked: Boolean
)

data class ShopBackgroundResponse(
    val userPoints: Int,
    val backgrounds: List<ShopBackgroundDto>
)
