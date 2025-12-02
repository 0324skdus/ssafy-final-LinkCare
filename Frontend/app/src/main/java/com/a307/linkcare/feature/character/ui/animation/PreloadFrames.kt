package com.a307.linkcare.feature.character.ui.animation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import com.a307.linkcare.feature.character.domain.model.CharacterManifest

@Composable
fun PreloadFrames(manifest: CharacterManifest, stateKey: String) {
    val ctx = LocalContext.current
    val imageLoader = LocalContext.current.imageLoader
    val s = manifest.states[stateKey] ?: return

    LaunchedEffect(manifest.characterId, stateKey) {
        repeat(s.frames) { idx ->
            val url = buildUrl(manifest.imageBaseUrl, s.path, s.pattern, idx)
            imageLoader.enqueue(
                ImageRequest.Builder(ctx)
                    .data(url)
                    .build()
            )
        }
    }
}

fun buildUrl(base: String, path: String, pattern: String, frameIndex: Int): String {
    val file = pattern.format(frameIndex) // e.g., frame_000.png
    return if (base.endsWith("/")) "$base$path/$file" else "$base/$path/$file"
}
