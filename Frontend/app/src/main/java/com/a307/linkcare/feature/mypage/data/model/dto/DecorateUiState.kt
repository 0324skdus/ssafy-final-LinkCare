package com.a307.linkcare.feature.mypage.data.model.dto

data class DecorateUiState(
    val ownedCharacters: List<CharacterDto> = emptyList(),
    val ownedBackgrounds: List<BackgroundDto> = emptyList(),
    val mainCharacter: CharacterDto? = null,
    val mainBackground: BackgroundDto? = null,
    val loading: Boolean = false
)