@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.mypage.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.LcBtn
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.util.loadCroppedPainterIO
import androidx.compose.runtime.mutableStateMapOf

/* ---------- 추가: 논리 ID → drawable ID 매핑 함수 ---------- */

fun toCharDrawable(id: Int): Int = when (id) {
    1 -> R.drawable.char_duck_1
    2 -> R.drawable.char_bear_1
    else -> R.drawable.char_duck_1
}

fun toBgDrawable(id: Int): Int = when (id) {
    1 -> R.drawable.background_1
    2 -> R.drawable.background_2
    else -> R.drawable.background_1
}
/* ---------- 여기서부터 기존 DecorateScreen 수정 ---------- */
@Composable
fun DecorateScreen(
    ownedCharacters: List<Int>,     // 논리 ID 리스트
    ownedBackgrounds: List<Int>,    // 논리 ID 리스트
    equippedCharacter: Int,         // 논리 ID
    equippedBackground: Int,        // 논리 ID
    modifier: Modifier = Modifier,
    onApply: (selectedCharId: Int, selectedBgId: Int) -> Unit = { _, _ -> }
) {
    var tab by rememberSaveable { mutableStateOf(StoreTab.CHARACTER) }

    // selChar / selBg 를 논리 ID 로 유지
    var selChar by rememberSaveable { mutableStateOf(equippedCharacter) }
    var selBg by rememberSaveable { mutableStateOf(equippedBackground) }

    val context = LocalContext.current

    /* ---------- 캐릭터 Painter 로딩: 논리 ID를 drawable로 변환 ---------- */
    val charPainters: MutableMap<Int, Painter> = remember { mutableStateMapOf() }

    LaunchedEffect(ownedCharacters) {
        ownedCharacters.forEach { id ->
            if (charPainters[id] == null) {
                charPainters[id] = loadCroppedPainterIO(context, toCharDrawable(id))
            }
        }
        if (charPainters[equippedCharacter] == null) {
            charPainters[equippedCharacter] =
                loadCroppedPainterIO(context, toCharDrawable(equippedCharacter))
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(white)
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {

            // 탭
            TabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = white,
                contentColor = main,
                indicator = { positions ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(positions[tab.ordinal]),
                        color = main
                    )
                }
            ) {
                Tab(selected = tab == StoreTab.CHARACTER, onClick = { tab = StoreTab.CHARACTER }) {
                    Text("캐릭터", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = tab == StoreTab.BACKGROUND, onClick = { tab = StoreTab.BACKGROUND }) {
                    Text("배경", modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(6.dp))

            CarouselBox(
                onPrev = {
                    when (tab) {
                        StoreTab.CHARACTER -> {
                            val idx = ownedCharacters.indexOf(selChar).coerceAtLeast(0)
                            selChar = ownedCharacters[(idx - 1).takeIf { it >= 0 }
                                ?: ownedCharacters.lastIndex]
                        }

                        StoreTab.BACKGROUND -> {
                            val idx = ownedBackgrounds.indexOf(selBg).coerceAtLeast(0)
                            selBg = ownedBackgrounds[(idx - 1).takeIf { it >= 0 }
                                ?: ownedBackgrounds.lastIndex]
                        }
                    }
                },

                onNext = {
                    when (tab) {
                        StoreTab.CHARACTER -> {
                            val idx = ownedCharacters.indexOf(selChar).coerceAtLeast(0)
                            selChar = ownedCharacters[(idx + 1) % ownedCharacters.size]
                        }

                        StoreTab.BACKGROUND -> {
                            val idx = ownedBackgrounds.indexOf(selBg).coerceAtLeast(0)
                            selBg = ownedBackgrounds[(idx + 1) % ownedBackgrounds.size]
                        }
                    }
                }
            ) {
                // Painter도 논리 ID → drawable로 변환해서 사용
                val selPainter = charPainters[selChar]
                    ?: rememberCroppedPainter(toCharDrawable(selChar))

//                CirclePreview(
//                    backgroundRes = toBgDrawable(selBg),
//                    characterPainter = selPainter,
//                )
            }

            Spacer(Modifier.height(12.dp))

            /* ---------- Apply 버튼: 논리 ID 그대로 전달 ---------- */
            LcBtn(
                text = "적용",
                modifier = Modifier.fillMaxWidth(),
                buttonColor = main,
                buttonTextColor = white,
                isEnabled = true,
                onClick = { onApply(selChar, selBg) } // 논리 ID 전달
            )
        }
    }
}
