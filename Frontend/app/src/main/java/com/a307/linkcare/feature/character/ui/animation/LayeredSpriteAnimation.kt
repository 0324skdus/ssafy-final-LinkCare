package com.a307.linkcare.feature.character.ui.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import com.a307.linkcare.feature.character.domain.model.CharacterManifest
import kotlinx.coroutines.delay

@Composable
fun LayeredSpriteAnimation(
    manifest: CharacterManifest,
    stateKey: String = "idle",
    layers: List<String> = manifest.layers ?: emptyList(),
    modifier: Modifier = Modifier
) {
    val s = manifest.states[stateKey] ?: return
    var frame by remember { mutableStateOf(0) }

    LaunchedEffect(manifest.characterId, stateKey) {
        val delayMs = (1000L / s.fps).coerceAtLeast(16L)
        while (true) {
            delay(delayMs)
            frame = (frame + 1) % s.frames
        }
    }

    Box(modifier = modifier) {
        // 순서대로 겹치기
        for (layer in layers) {
            val url = buildUrl(manifest.imageBaseUrl, "layers/$layer", s.pattern, frame)
            AsyncImage(model = url, contentDescription = null)
        }
    }
}
