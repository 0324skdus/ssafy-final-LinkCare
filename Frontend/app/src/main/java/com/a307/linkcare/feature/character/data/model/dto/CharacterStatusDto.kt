package com.a307.linkcare.feature.character.data.model.dto

import com.a307.linkcare.feature.character.domain.model.CharacterManifest
import kotlinx.serialization.Serializable

@Serializable
data class CharacterStatusDto(
    val characterId: Long,
    val name: String,
    val description: String? = null,
    val imageUrls: List<String> = emptyList(),
    val isUnlocked: Boolean,
    val isMain: Boolean,
    val manifest: CharacterManifest? = null
)