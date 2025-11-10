@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.mypage

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.LcBtn
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.util.loadCroppedPainterIO
import androidx.compose.runtime.mutableStateMapOf


@Composable
fun DecorateScreen(
    ownedCharacters: List<Int>,
    ownedBackgrounds: List<Int>,
    @DrawableRes equippedCharacter: Int,
    @DrawableRes equippedBackground: Int,
    modifier: Modifier = Modifier,
    onApply: (selectedCharRes: Int, selectedBgRes: Int) -> Unit = { _, _ -> }
) {
    var tab by rememberSaveable { mutableStateOf(StoreTab.CHARACTER) }
    var selChar by rememberSaveable { mutableStateOf(equippedCharacter) }
    var selBg by rememberSaveable { mutableStateOf(equippedBackground) }

    val context = LocalContext.current

    val charPainters: MutableMap<Int, Painter> = remember { mutableStateMapOf() }
    LaunchedEffect(ownedCharacters) {
        ownedCharacters.forEach { res ->
            if (charPainters[res] == null) charPainters[res] = loadCroppedPainterIO(context, res)
        }
        if (charPainters[equippedCharacter] == null) {
            charPainters[equippedCharacter] = loadCroppedPainterIO(context, equippedCharacter)
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(white)
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {

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
                        StoreTab.CHARACTER -> if (ownedCharacters.isNotEmpty()) {
                            val idx = ownedCharacters.indexOf(selChar).coerceAtLeast(0)
                            selChar = ownedCharacters[if (idx - 1 < 0) ownedCharacters.lastIndex else idx - 1]
                        }
                        StoreTab.BACKGROUND -> if (ownedBackgrounds.isNotEmpty()) {
                            val idx = ownedBackgrounds.indexOf(selBg).coerceAtLeast(0)
                            selBg = ownedBackgrounds[if (idx - 1 < 0) ownedBackgrounds.lastIndex else idx - 1]
                        }
                    }
                },
                onNext = {
                    when (tab) {
                        StoreTab.CHARACTER -> if (ownedCharacters.isNotEmpty()) {
                            val idx = ownedCharacters.indexOf(selChar).coerceAtLeast(0)
                            selChar = ownedCharacters[(idx + 1) % ownedCharacters.size]
                        }
                        StoreTab.BACKGROUND -> if (ownedBackgrounds.isNotEmpty()) {
                            val idx = ownedBackgrounds.indexOf(selBg).coerceAtLeast(0)
                            selBg = ownedBackgrounds[(idx + 1) % ownedBackgrounds.size]
                        }
                    }
                }
            ) {
                val selPainter: Painter? = charPainters[selChar] ?: rememberCroppedPainter(selChar)

                CirclePreview(
                    backgroundRes = selBg,
                    characterPainter = selPainter,
                )
            }

            Spacer(Modifier.height(12.dp))

            LcBtn(
                text = "적용",
                modifier = Modifier.fillMaxWidth(),
                buttonColor = main,
                buttonTextColor = white,
                isEnabled = true,
                onClick = { onApply(selChar, selBg) }
            )
        }
    }
}