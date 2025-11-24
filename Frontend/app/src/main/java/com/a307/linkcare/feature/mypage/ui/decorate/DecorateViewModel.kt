package com.a307.linkcare.feature.mypage.ui.decorate

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.mypage.domain.repository.DecorateRepository
import com.a307.linkcare.feature.mypage.data.model.dto.DecorateUiState
import com.a307.linkcare.feature.watch.manager.DataLayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DecorateViewModel @Inject constructor(
    private val repository: DecorateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecorateUiState(loading = true))
    val uiState: StateFlow<DecorateUiState> = _uiState

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }

            runCatching {
                val chars = repository.getCharacters()
                val mainChar = repository.getMainCharacter()

                val bgs = repository.getBackgrounds()
                val mainBg = repository.getMainBackground()

                _uiState.update {
                    DecorateUiState(
                        ownedCharacters = chars,
                        ownedBackgrounds = bgs,
                        mainCharacter = mainChar,
                        mainBackground = mainBg,
                        loading = false
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun applyDecoration(charId: Long, bgId: Long) {
        viewModelScope.launch {
            try {
                if (charId != 0L) {
                    repository.setMainCharacter(charId)
                }
                if (bgId != 0L) {
                    repository.setMainBackground(bgId)
                }
            } catch (e: Exception) {
                Log.e("DecorateViewModel", "applyDecoration error", e)
            }
        }
    }



    fun applyDecoration(charId: Long, bgId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.setMainCharacter(charId)
                repository.setMainBackground(bgId)
            }.onSuccess {
                loadAll()
                onSuccess()
            }
        }
    }

    fun sendToWear(context: Context, charId: Long, bgId: Long) {
        DataLayerManager.sendTheme(context, charId.toInt(), bgId.toInt())
    }
}
