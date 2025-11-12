@file:OptIn(ExperimentalFoundationApi::class)

package com.a307.linkcare.feature.auth.ui

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.PermissionController
import androidx.hilt.navigation.compose.hiltViewModel
import com.a307.linkcare.R
import com.a307.linkcare.common.component.atoms.LcBtn
import com.a307.linkcare.common.component.atoms.LcInputField
import com.a307.linkcare.common.component.molecules.PagerDots
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.common.util.loadCroppedPainterIO
import com.a307.linkcare.feature.auth.HealthPermissionViewModel
import com.a307.linkcare.feature.auth.data.HealthPermissions
import kotlinx.coroutines.launch

@Composable
fun SignupCharacterScreen(
    pagerState: PagerState,
    pageCount: Int,
    @DrawableRes characterImages: List<Int> = listOf(
        R.drawable.char_bear_1,
        R.drawable.char_bear_2,
        R.drawable.char_bear_3,
        R.drawable.char_duck_1,
        R.drawable.char_duck_2
    ),
    onStart: (selectedIndex: Int, name: String) -> Unit = { _, _ -> },
    onNavigateToIntro: () -> Unit = {},
    healthVm: HealthPermissionViewModel = hiltViewModel(),
) {
    require(characterImages.isNotEmpty()) { "characterImages must not be empty" }

    val scope = rememberCoroutineScope()
    var selectedIndex by rememberSaveable { mutableStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    val canStart = name.isNotBlank()
    var showPermissionDialog by rememberSaveable { mutableStateOf(false) }
    var shouldSubmitProfile by remember { mutableStateOf(false) }

    // --- 권한 처리 준비 ---
    val context = LocalContext.current
    val healthState by healthVm.state.collectAsState()

    // Health Connect 권한 요청 런처
    val permissionLauncher =
        rememberLauncherForActivityResult<Set<String>, Set<String>>(
            PermissionController.createRequestPermissionResultContract()
        ) { granted: Set<String> ->
            if (HealthPermissions.permissions.all { it in granted }) {
                // 권한 승인 후 프로필 제출 트리거
                shouldSubmitProfile = true
                // 권한 상태 재확인하여 State.Granted로 전이 유도
                healthVm.checkPermissions()
            }
        }

    // VM 상태 변화 또는 제출 플래그 변화 시, 실제 권한 요청/제출 실행
    LaunchedEffect(healthState, shouldSubmitProfile) {
        if (shouldSubmitProfile) {
            when (healthState) {
                HealthPermissionViewModel.State.NeedPermission -> {
                    permissionLauncher.launch(HealthPermissions.permissions)
                }
                HealthPermissionViewModel.State.Granted -> {
                    // 권한이 있으면 바로 프로필 제출
                    onStart(selectedIndex, name) // 선택한 캐릭터 인덱스와 이름
                    shouldSubmitProfile = false
                }
                else -> Unit
            }
        }
    }

    // 최초 1회 권한 상태 확인
    LaunchedEffect(Unit) {
        healthVm.checkPermissions()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(white)
    ) {
        // 상단 도트
        PagerDots(
            total = pageCount,
            current = pagerState.currentPage,
            onDotClick = { index -> scope.launch { pagerState.animateScrollToPage(index) } }
        )

        // 본문
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 제목/부제
            Text(
                text = "캐릭터를 골라주세요",
                fontSize = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "* 앞으로 나의 건강 상태를 표현할 캐릭터입니다.",
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            // 캐릭터 이미지 Painter 캐시
            val cachedPainters = remember(characterImages) {
                mutableStateListOf<BitmapPainter?>().also { list ->
                    repeat(characterImages.size) { list.add(null) }
                }
            }

            // IO 스레드에서 미리 로드 + 크롭 + 캐싱
            LaunchedEffect(Unit) {
                characterImages.forEachIndexed { i, resId ->
                    if (cachedPainters[i] == null) {
                        cachedPainters[i] = loadCroppedPainterIO(context, resId)
                    }
                }
            }

            // 새 그림이 준비되기 전엔 직전 그림을 유지
            var lastReadyIndex by remember { mutableStateOf(0) }
            val currentPainter = cachedPainters.getOrNull(selectedIndex)
            val painterToShow = currentPainter ?: cachedPainters.getOrNull(lastReadyIndex)

            LaunchedEffect(currentPainter, selectedIndex) {
                if (currentPainter != null) lastReadyIndex = selectedIndex
            }

            // ───────────────────── 캐릭터 선택 박스 ─────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                // 좌측 화살표
                ArrowPill(
                    text = "〈 ",
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp)
                ) {
                    selectedIndex =
                        if (selectedIndex - 1 < 0) characterImages.lastIndex else selectedIndex - 1
                }

                // 원형 프레임
                Box(
                    modifier = Modifier
                        .size(255.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF3FF))
                        .border(3.dp, Color(0xFFE5E7EB), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // 배경 이미지
                    Image(
                        painter = painterResource(id = R.drawable.background_1),
                        contentDescription = "character background",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop
                    )

                    // 빠른 크로스페이드 + 크롭된 이미지 표시
                    if (painterToShow != null) {
                        Crossfade(
                            targetState = painterToShow,
                            animationSpec = tween(durationMillis = 0),
                            label = "charCrossfade_fast"
                        ) { painter ->
                            Image(
                                painter = painter,
                                contentDescription = "character",
                                modifier = Modifier
                                    .fillMaxSize(0.5f)
                                    .align(Alignment.Center)
                                    .offset(y = 10.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                // 우측 화살표
                ArrowPill(
                    text = " 〉",
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                ) {
                    selectedIndex = (selectedIndex + 1) % characterImages.size
                }
            }

            Spacer(Modifier.height(16.dp))

            // 캐릭터명 입력
            LcInputField(
                value = name,
                onValueChange = { name = it },
                placeholder = "캐릭터명을 입력해주세요",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            )

            Spacer(Modifier.height(14.dp))

            // 시작하기 버튼
            LcBtn(
                text = "시작하기",
                modifier = Modifier.fillMaxWidth(),
                buttonColor = if (canStart) main else unActiveBtn,
                buttonTextColor = if (canStart) white else unActiveField,
                isEnabled = canStart,
                onClick = {
                    onStart(0, "duck")
                    showPermissionDialog = true
                }
            )
        }

        // Samsung Health 권한 팝업
        if (showPermissionDialog) {
            SamsungHealthPermissionDialog(
                onDismiss = { showPermissionDialog = false },
                onConnectClick = {
                    showPermissionDialog = false
                    shouldSubmitProfile = true
                    // 권한 체크 시작
                    healthVm.checkPermissions()
                }
            )
        }
    }
}

// ───────────────────────── ArrowPill ─────────────────────────
@Composable
private fun ArrowPill(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .size(width = 36.dp, height = 36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFF3F4F6))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6B7280)
        )
    }
}