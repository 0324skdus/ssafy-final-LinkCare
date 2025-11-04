package com.a307.linkcare.avatarTest

import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun AnimationTest() {
//    AvatarAnimator(
//        modifier = Modifier
//            .graphicsLayer(scaleX = 1.2f, scaleY = 1.2f) // 전체 확대
//            .offset(x = 12.dp, y = (-8).dp),              // 전체 위치 이동
//        selection = AvatarSelection(
//            species = Species.DUCK,
//            variant = 1,
//            hatId = "brown_hat",
//            glassesId = "blue",
//            animation = "walk"
//        ),
//        fps = 25
//    )

    AvatarAnimator(
        modifier = Modifier
            .graphicsLayer(scaleX = 1.2f, scaleY = 1.2f) // 전체 확대
            .offset(x = 12.dp, y = (-8).dp),              // 전체 위치 이동
        selection = AvatarSelection(
            species = Species.BEAR,
            variant = 1,
            hatId = "blue",
            glassesId = "blue",
            animation = "walk"
        ),
        fps = 25
    )
}