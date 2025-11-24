package com.a307.linkcare.feature.character.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.a307.linkcare.feature.character.ui.animation.PreloadFrames
import com.a307.linkcare.feature.character.ui.animation.SpriteAnimation
import com.a307.linkcare.feature.character.ui.viewmodel.CharacterViewModel

@Composable
fun CharacterScreen(
    vm: CharacterViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val s = vm.state
    LaunchedEffect(Unit) { vm.load() }

    when {
        s.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        s.error != null -> Text("에러: ${s.error}")
        else -> {
            LazyColumn {
                // 메인 캐릭터
                item {
                    s.main?.manifest?.let { manifest ->
                        PreloadFrames(manifest, "idle")
                        SpriteAnimation(
                            manifest = manifest,
                            stateKey = "idle",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                }
                // 목록
                items(s.items) { item ->
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 썸네일
                        AsyncImage(
                            model = item.imageUrls.firstOrNull(),
                            contentDescription = item.name,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, fontWeight = FontWeight.Bold)
                            Text(item.description ?: "")
                            Text(if (item.isUnlocked) "해금됨" else "잠금")
                            if (item.isMain) Text("메인", color = Color(0xFF4A89F6))
                        }
                        // 액션 버튼들
                        if (!item.isUnlocked) {
                            Button(onClick = { vm.unlock(item.characterId) }) { Text("해금") }
                        } else if (!item.isMain) {
                            // userCharacterId를 목록에 포함해주면 여기서 사용
                            Button(onClick = { /* vm.setMain(userCharacterId) */ }) { Text("메인지정") }
                        }
                    }
                }
            }
        }
    }
}
