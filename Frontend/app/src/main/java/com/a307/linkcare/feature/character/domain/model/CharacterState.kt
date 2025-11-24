package com.a307.linkcare.feature.character.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CharacterState(
    val fps: Int,
    val path: String,              // ex) "idle", "walk"
    val pattern: String,           // ex) "frame_%03d.png"
    val frames: Int
)