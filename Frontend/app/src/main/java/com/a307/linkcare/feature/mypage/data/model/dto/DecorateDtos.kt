package com.a307.linkcare.feature.mypage.data.model.dto

data class CharacterDto(
    val characterId: Long,
    val name: String,
    val description: String,
    val baseImageUrl: String,
    val animatedImageUrl: String,
    val unlocked: Boolean,
    val main: Boolean
)

data class BackgroundDto(
    val backgroundId: Long,
    val name: String,
    val description: String,
    val imageUrl: String,
    val unlocked: Boolean,
    val main: Boolean
)