package com.a307.linkcare.feature.mypage.ui.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.mypage.data.model.dto.StoreItem
import com.a307.linkcare.feature.mypage.data.model.dto.StoreUiState
import com.a307.linkcare.feature.mypage.domain.repository.ShopRepository
import com.a307.linkcare.feature.mypage.ui.state.StoreEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StoreViewModel @Inject constructor(
    private val repo: ShopRepository
) : ViewModel() {

    var uiState by mutableStateOf(
        StoreUiState(
            coins = 0,
            characterItems = emptyList(),
            backgroundItems = emptyList(),
            equippedBackground = "",
            equippedCharacter = ""
        )
    )
        private set

    var loading by mutableStateOf(false)
        private set

    // ---- 이벤트 플로우
    private val _eventFlow = MutableSharedFlow<StoreEvent>()
    val eventFlow = _eventFlow.asSharedFlow()


    init {
        loadStore()
    }

    fun loadStore() {
        viewModelScope.launch {
            loading = true
            try {
                val chars = repo.loadCharacters()
                val bgs = repo.loadBackgrounds()

                uiState = uiState.copy(
                    coins = chars.userPoints,
                    characterItems = chars.characters.map {
                        StoreItem(
                            id = it.characterId,
                            imageUrl = it.baseImageUrl,
                            animatedImageUrl = it.animatedImageUrl,
                            price = it.price,
                            owned = it.unlocked
                        )
                    },
                    backgroundItems = bgs.backgrounds.map {
                        StoreItem(
                            id = it.backgroundId,
                            imageUrl = it.imageUrl,
                            price = it.price,
                            owned = it.unlocked
                        )
                    }
                )
            } catch (e: Exception) {
            } finally {
                loading = false
            }
        }
    }


    fun buy(item: StoreItem, type: StoreTab) {
        viewModelScope.launch {
            // --- 코인 부족
            if (uiState.coins < item.price) {
                _eventFlow.emit(StoreEvent.NotEnoughCoins)
                return@launch
            }

            loading = true
            try {

                // --- 서버 구매 요청
                if (type == StoreTab.CHARACTER)
                    repo.buyCharacter(item.id)
                else
                    repo.buyBackground(item.id)

                // --- 즉시 UI 반영
                uiState = when (type) {
                    StoreTab.CHARACTER -> {
                        uiState.copy(
                            coins = uiState.coins - item.price,
                            characterItems = uiState.characterItems.map {
                                if (it.id == item.id) it.copy(owned = true) else it
                            }
                        )
                    }

                    StoreTab.BACKGROUND -> {
                        uiState.copy(
                            coins = uiState.coins - item.price,
                            backgroundItems = uiState.backgroundItems.map {
                                if (it.id == item.id) it.copy(owned = true) else it
                            }
                        )
                    }
                }

                // --- 구매 성공 이벤트 발생
                _eventFlow.emit(
                    StoreEvent.PurchaseSuccess(
                        itemName = item.imageUrl.substringAfterLast("/")
                    )
                )

            } catch (e: Exception) {
                _eventFlow.emit(StoreEvent.PurchaseFailed("구매 실패"))
            } finally {
                loading = false
            }
        }
    }
}
