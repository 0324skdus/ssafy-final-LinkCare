@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.mypage.ui.decorate

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.a307.linkcare.common.component.atoms.LcBtn
import com.a307.linkcare.common.theme.main
import com.a307.linkcare.common.theme.white
import com.a307.linkcare.feature.mypage.data.model.dto.BackgroundDto
import com.a307.linkcare.feature.mypage.data.model.dto.CharacterDto
import com.a307.linkcare.feature.mypage.ui.store.CarouselBox
import com.a307.linkcare.feature.mypage.ui.store.CirclePreview
import com.a307.linkcare.feature.mypage.ui.store.StoreTab

@Composable
fun DecorateScreen(
    ownedCharacters: List<CharacterDto>,
    ownedBackgrounds: List<BackgroundDto>,
    equippedCharacter: CharacterDto?,
    equippedBackground: BackgroundDto?,
    modifier: Modifier = Modifier,
    onApply: (Long, Long) -> Unit = { _, _ -> }
) {
    var tab by rememberSaveable { mutableStateOf(StoreTab.CHARACTER) }

    var selectedCharacterIndex by rememberSaveable { mutableIntStateOf(0) }
    var selectedBackgroundIndex by rememberSaveable { mutableIntStateOf(0) }

    var selectedCharId by rememberSaveable { mutableLongStateOf(equippedCharacter?.characterId ?: 0L) }
    var selectedBgId by rememberSaveable { mutableLongStateOf(equippedBackground?.backgroundId ?: 0L) }

    val itemsCharacter = ownedCharacters.filter { it.unlocked }
    val itemsBackground = ownedBackgrounds.filter { it.unlocked }

    val items: List<Any> = when (tab) {
        StoreTab.CHARACTER -> itemsCharacter
        StoreTab.BACKGROUND -> itemsBackground
    }

    val selectedIndex = when (tab) {
        StoreTab.CHARACTER -> selectedCharacterIndex
        StoreTab.BACKGROUND -> selectedBackgroundIndex
    }

    val previewBgUrl =
        if (tab == StoreTab.BACKGROUND && items.isNotEmpty())
            (items[selectedIndex] as BackgroundDto).imageUrl
        else equippedBackground?.imageUrl ?: ""

    val previewCharUrl =
        if (tab == StoreTab.CHARACTER && items.isNotEmpty())
            (items[selectedIndex] as CharacterDto).baseImageUrl
        else equippedCharacter?.baseImageUrl ?: ""

    val isApplyEnabled = when (tab) {
        StoreTab.CHARACTER -> {
            if (itemsCharacter.isEmpty()) false
            else !itemsCharacter[selectedCharacterIndex].main
        }
        StoreTab.BACKGROUND -> {
            if (itemsBackground.isEmpty()) false
            else !itemsBackground[selectedBackgroundIndex].main
        }
    }

    fun updateSelectedId() {
        when (tab) {
            StoreTab.CHARACTER -> {
                if (itemsCharacter.isNotEmpty()) {
                    selectedCharId = itemsCharacter[selectedCharacterIndex].characterId
                }
            }
            StoreTab.BACKGROUND -> {
                if (itemsBackground.isNotEmpty()) {
                    selectedBgId = itemsBackground[selectedBackgroundIndex].backgroundId
                }
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .background(white)
            .padding(horizontal = 16.dp)
    ) {
        Column {

            Spacer(Modifier.height(6.dp))

            // 탭 전환
            TabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = white,
                contentColor = main,
                indicator = { pos ->
                    TabRowDefaults.Indicator(
                        Modifier.tabIndicatorOffset(pos[tab.ordinal]),
                        color = main
                    )
                }
            ) {
                Tab(
                    selected = tab == StoreTab.CHARACTER,
                    onClick = {
                        tab = StoreTab.CHARACTER
                        selectedCharacterIndex = 0
                        updateSelectedId()
                    }
                ) { Text("캐릭터", Modifier.padding(12.dp)) }

                Tab(
                    selected = tab == StoreTab.BACKGROUND,
                    onClick = {
                        tab = StoreTab.BACKGROUND
                        selectedBackgroundIndex = 0
                        updateSelectedId()
                    }
                ) { Text("배경", Modifier.padding(12.dp)) }
            }

            Spacer(Modifier.height(6.dp))

            // 캐러셀
            CarouselBox(
                onPrev = {
                    if (items.isEmpty()) return@CarouselBox
                    when (tab) {
                        StoreTab.CHARACTER -> {
                            selectedCharacterIndex =
                                if (selectedCharacterIndex - 1 < 0) itemsCharacter.lastIndex
                                else selectedCharacterIndex - 1
                        }

                        StoreTab.BACKGROUND -> {
                            selectedBackgroundIndex =
                                if (selectedBackgroundIndex - 1 < 0) itemsBackground.lastIndex
                                else selectedBackgroundIndex - 1
                        }
                    }
                    updateSelectedId()
                },
                onNext = {
                    if (items.isEmpty()) return@CarouselBox
                    when (tab) {
                        StoreTab.CHARACTER -> {
                            selectedCharacterIndex =
                                (selectedCharacterIndex + 1) % itemsCharacter.size
                        }

                        StoreTab.BACKGROUND -> {
                            selectedBackgroundIndex =
                                (selectedBackgroundIndex + 1) % itemsBackground.size
                        }
                    }
                    updateSelectedId()
                }
            ) {
                CirclePreview(
                    backgroundUrl = previewBgUrl,
                    characterUrl = previewCharUrl
                )
            }

            Spacer(Modifier.height(20.dp))

            // 적용 버튼
            LcBtn(
                text = "적용",
                modifier = Modifier.fillMaxWidth(),
                buttonColor = main,
                buttonTextColor = white,
                isEnabled = isApplyEnabled,
                onClick = {
                    if (items.isEmpty()) return@LcBtn
                    when (tab) {
                        StoreTab.CHARACTER -> onApply(selectedCharId, 0L)
                        StoreTab.BACKGROUND -> onApply(0L, selectedBgId)
                    }
                }
            )
        }
    }
}
