package com.a307.linkcare.feature.mypage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.auth.data.RetrofitClient
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MyPageApiState {
    object Loading : MyPageApiState
    data class Success(val uiState: MyPageUiState) : MyPageApiState
    data class Error(val message: String) : MyPageApiState
}

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val repo: MyPageRepository
) : ViewModel() {

    private val _apiState = MutableStateFlow<MyPageApiState>(MyPageApiState.Loading)
    val apiState: StateFlow<MyPageApiState> = _apiState

    init {
        fetchMyPageData()
    }

    fun fetchMyPageData() {
        viewModelScope.launch {
            _apiState.value = MyPageApiState.Loading
            try {
                val response = RetrofitClient.userApi.getMyInfo()
                if (response.isSuccessful && response.body() != null) {
                    val userData = response.body()!!
                    val avatarUrl = userData.mainCharacterImageUrl
                    val avatarBgUrl = userData.mainBackgroundImageUrl

                    val uiState = MyPageUiState(
                        nickname = userData.nickname,
                        coinLabel = "보유 코인 ${userData.points}",
                        groupCountLabel = "그룹 조회",
                        avatarUrl = avatarUrl,
                        avatarBgUrl = avatarBgUrl
                    )
                    _apiState.value = MyPageApiState.Success(uiState)
                } else {
                    val errorMsg = "정보를 불러오지 못했습니다: ${response.code()}"
                    Log.e("MyPageViewModel", errorMsg)
                    _apiState.value = MyPageApiState.Error(errorMsg)
                }
            } catch (e: Exception) {
                val errorMsg = "서버 통신 오류: ${e.message}"
                Log.e("MyPageViewModel", errorMsg, e)
                _apiState.value = MyPageApiState.Error(errorMsg)
            }
        }
    }

    private val _groupCharacters = MutableStateFlow<List<GroupCharacterResponse>>(emptyList())
    val groupCharacters: StateFlow<List<GroupCharacterResponse>> = _groupCharacters

    private val _groupCharacterLoading = MutableStateFlow(false)
    val groupCharacterLoading: StateFlow<Boolean> = _groupCharacterLoading

    private val _groupCharacterError = MutableStateFlow<String?>(null)
    val groupCharacterError: StateFlow<String?> = _groupCharacterError


    fun loadGroupCharacters(groupSeq: Long) {
        viewModelScope.launch {
            _groupCharacterLoading.value = true
            _groupCharacterError.value = null

            try {
                val res = repo.getMyGroupCharacters(groupSeq)

                val uiList = res.members.map { it }

                _groupCharacters.value = uiList

            } catch (e: Exception) {
                Log.e("MyPageViewModel", "loadGroupCharacters error", e)
                _groupCharacterError.value = e.message
            } finally {
                _groupCharacterLoading.value = false
            }
        }
    }

}
