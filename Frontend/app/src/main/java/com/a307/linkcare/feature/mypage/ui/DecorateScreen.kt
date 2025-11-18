@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.a307.linkcare.feature.mypage.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.a307.linkcare.common.component.atoms.LcBtn
import com.a307.linkcare.common.theme.*
import com.a307.linkcare.feature.mypage.domain.CharacterDto
import com.a307.linkcare.feature.mypage.domain.BackgroundDto

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

    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    var selectedCharId by rememberSaveable { mutableLongStateOf(equippedCharacter?.characterId ?: 0L) }
    var selectedBgId by rememberSaveable { mutableLongStateOf(equippedBackground?.backgroundId ?: 0L) }

    val items = when (tab) {
        StoreTab.CHARACTER -> ownedCharacters
        StoreTab.BACKGROUND -> ownedBackgrounds
    }

    if (items.isNotEmpty() && selectedIndex !in items.indices) {
        selectedIndex = 0
    }

    val previewBgUrl =
        if (tab == StoreTab.BACKGROUND && items.isNotEmpty())
            (items[selectedIndex] as BackgroundDto).imageUrl
        else equippedBackground?.imageUrl ?: ""

    val previewCharUrl =
        if (tab == StoreTab.CHARACTER && items.isNotEmpty())
            (items[selectedIndex] as CharacterDto).baseImageUrl
        else equippedCharacter?.baseImageUrl ?: ""

    Box(
        modifier
            .fillMaxSize()
            .background(white)
            .padding(horizontal = 16.dp)
    ) {
        Column {

            Spacer(Modifier.height(6.dp))

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
                    selected = (tab == StoreTab.CHARACTER),
                    onClick = { tab = StoreTab.CHARACTER }
                ) { Text("캐릭터", Modifier.padding(12.dp)) }

                Tab(
                    selected = (tab == StoreTab.BACKGROUND),
                    onClick = { tab = StoreTab.BACKGROUND }
                ) { Text("배경", Modifier.padding(12.dp)) }
            }

            Spacer(Modifier.height(6.dp))

            CarouselBox(
                onPrev = {
                    if (items.isEmpty()) return@CarouselBox
                    selectedIndex =
                        if (selectedIndex - 1 < 0) items.lastIndex else selectedIndex - 1

                    if (tab == StoreTab.CHARACTER)
                        selectedCharId = (items[selectedIndex] as CharacterDto).characterId
                    else
                        selectedBgId = (items[selectedIndex] as BackgroundDto).backgroundId
                },
                onNext = {
                    if (items.isEmpty()) return@CarouselBox
                    selectedIndex = (selectedIndex + 1) % items.size

                    if (tab == StoreTab.CHARACTER)
                        selectedCharId = (items[selectedIndex] as CharacterDto).characterId
                    else
                        selectedBgId = (items[selectedIndex] as BackgroundDto).backgroundId
                }
            ) {
                CirclePreview(
                    backgroundUrl = previewBgUrl,
                    characterUrl = previewCharUrl
                )
            }

            Spacer(Modifier.height(20.dp))

            LcBtn(
                text = "적용",
                modifier = Modifier.fillMaxWidth(),
                buttonColor = main,
                buttonTextColor = white,
                isEnabled = selectedCharId != 0L && selectedBgId != 0L,
                onClick = { onApply(selectedCharId, selectedBgId) }
            )
        }
    }
}


private fun updateSelection(
    tab: StoreTab,
    items: List<Any>,
    index: Int,
    onCharacter: (CharacterDto) -> Unit,
    onBackground: (BackgroundDto) -> Unit
) {
    if (tab == StoreTab.CHARACTER) onCharacter(items[index] as CharacterDto)
    else onBackground(items[index] as BackgroundDto)
}
