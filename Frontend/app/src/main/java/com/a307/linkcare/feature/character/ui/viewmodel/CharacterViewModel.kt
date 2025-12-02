package com.a307.linkcare.feature.character.ui.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.character.data.model.dto.CharacterStatusDto
import com.a307.linkcare.feature.character.domain.repository.CharacterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterViewModel @Inject constructor(
    private val repo: CharacterRepository
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        val items: List<CharacterStatusDto> = emptyList(),
        val main: CharacterStatusDto? = null,
        val error: String? = null
    )

    var state by mutableStateOf(UiState())
        private set

    fun load() = viewModelScope.launch {
        state = state.copy(loading = true, error = null)
        try {
            val list = repo.getCharacters()
            val main = repo.getMain()
            state = state.copy(loading = false, items = list, main = main)
        } catch (e: Exception) {
            state = state.copy(loading = false, error = e.message)
        }
    }

    fun selectInitial(characterId: Long) = viewModelScope.launch {
        try {
            repo.selectInitial(characterId)
            load()
        } catch (e: Exception) {
            state = state.copy(error = e.message)
        }
    }

    fun setMain(userCharacterId: Long) = viewModelScope.launch {
        try {
            repo.setMain(userCharacterId)
            load()
        } catch (e: Exception) {
            state = state.copy(error = e.message)
        }
    }

    fun unlock(characterId: Long) = viewModelScope.launch {
        try {
            repo.unlock(characterId)
            load()
        } catch (e: Exception) {
            state = state.copy(error = e.message)
        }
    }
}