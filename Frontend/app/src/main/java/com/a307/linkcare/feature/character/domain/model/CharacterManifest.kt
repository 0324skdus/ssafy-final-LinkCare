package com.a307.linkcare.feature.character.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CharacterManifest(
    val characterId: Long,
    val name: String,
    val description: String? = null,
    val imageBaseUrl: String,            // ex) https://cdn.linkcare.com/characters/2-bear/
    val states: Map<String, CharacterState> = emptyMap(),
    val layers: List<String>? = null     // 레이어가 있으면 사용
)