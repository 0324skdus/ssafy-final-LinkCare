@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.mypage.ui.store

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.a307.linkcare.common.util.transformation.CropTransparentTransformation
import com.a307.linkcare.feature.mypage.data.model.dto.StoreItem
import com.a307.linkcare.feature.mypage.data.model.dto.StoreUiState

enum class StoreTab { CHARACTER, BACKGROUND }

/* --------- 상점 화면 --------- */
@Composable
fun StoreScreen(
    state: StoreUiState,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onBuy: (StoreItem, StoreTab) -> Unit
) {
    var tab by rememberSaveable { mutableStateOf(StoreTab.CHARACTER) }
    var selectedIndex by rememberSaveable { mutableStateOf(0) }

    val items = when (tab) {
        StoreTab.CHARACTER -> state.characterItems
        StoreTab.BACKGROUND -> state.backgroundItems
    }

    if (items.isNotEmpty() && selectedIndex !in items.indices) {
        selectedIndex = 0
    }

    val previewBgUrl =
        if (tab == StoreTab.BACKGROUND && items.isNotEmpty()) items[selectedIndex].imageUrl
        else state.equippedBackground

    val previewCharUrl =
        if (tab == StoreTab.CHARACTER && items.isNotEmpty()) items[selectedIndex].imageUrl
        else state.equippedCharacter


    Box(
        modifier
            .fillMaxSize()
            .background(white)
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.fillMaxSize()) {

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.coin), null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("보유 코인 ${state.coins}", color = Color(0xFF666666))
            }

            Spacer(Modifier.height(8.dp))

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
                Tab(
                    selected = tab == StoreTab.CHARACTER,
                    onClick = { tab = StoreTab.CHARACTER }
                ) {
                    Text("캐릭터", modifier = Modifier.padding(12.dp))
                }

                Tab(
                    selected = tab == StoreTab.BACKGROUND,
                    onClick = { tab = StoreTab.BACKGROUND }
                ) {
                    Text("배경", modifier = Modifier.padding(12.dp))
                }
            }

            Spacer(Modifier.height(6.dp))

            CarouselBox(
                onPrev = {
                    if (items.isNotEmpty())
                        selectedIndex =
                            if (selectedIndex - 1 < 0) items.lastIndex else selectedIndex - 1
                },
                onNext = {
                    if (items.isNotEmpty())
                        selectedIndex = (selectedIndex + 1) % items.size
                }
            ) {
                CirclePreview(
                    backgroundUrl = previewBgUrl,
                    characterUrl = previewCharUrl
                )
            }

            Spacer(Modifier.height(12.dp))

            if (items.isNotEmpty()) {
                val cur = items[selectedIndex]

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (!cur.owned) {
                        Image(painterResource(R.drawable.coin), null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${cur.price}", color = Color(0xFF666666))
                    }
                }

                Spacer(Modifier.height(10.dp))

                LcBtn(
                    text = if (cur.owned) "보유중" else "구매",
                    modifier = Modifier.fillMaxWidth(),
                    buttonColor = if (cur.owned) unActiveBtn else main,
                    buttonTextColor = if (cur.owned) unActiveField else white,
                    isEnabled = !cur.owned,
                    onClick = { onBuy(cur, tab) }
                )
            } else {
                Text(
                    "판매 중인 아이템이 없어요",
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* --------- 공통: 원형 미리보기 --------- */
@Composable
fun CirclePreview(
    backgroundUrl: String,
    characterUrl: String?,
    size: Dp = 255.dp
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFEFF3FF))
                .border(3.dp, Color(0xFFE5E7EB), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // 배경 이미지
            if (backgroundUrl.isNotBlank()) {
                AsyncImage(
                    model = backgroundUrl,
                    contentDescription = "background",
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop
                )
            }

            val context = LocalContext.current
            val characterSize = size * 0.55f
            // 캐릭터 이미지
            if (characterUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(characterUrl)
                        .crossfade(true)
                        .size(1024)
                        .transformations(CropTransparentTransformation())
                        .build(),
                    contentDescription = "avatar",
                    modifier = Modifier
                        .size(characterSize)
                        .offset(y = 12.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

/* --------- 공통: 캐러셀 컨테이너 + 화살표 --------- */
@Composable
fun CarouselBox(
    modifier: Modifier = Modifier,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        ArrowPill("〈 ", Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) { onPrev() }
        content()
        ArrowPill(" 〉", Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) { onNext() }
    }
}

@Composable
private fun ArrowPill(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF3F4F6))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6B7280))
    }
}