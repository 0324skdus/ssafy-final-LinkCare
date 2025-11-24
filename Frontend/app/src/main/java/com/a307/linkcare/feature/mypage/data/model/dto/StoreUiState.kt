package com.a307.linkcare.feature.mypage.data.model.dto

data class StoreUiState(
    val coins: Int,
    val characterItems: List<StoreItem>,
    val backgroundItems: List<StoreItem>,
    val equippedBackground: String,
    val equippedCharacter: String,
)