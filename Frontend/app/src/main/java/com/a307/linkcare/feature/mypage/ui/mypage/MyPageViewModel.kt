package com.a307.linkcare.feature.mypage.ui.mypage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.common.network.RetrofitClient
import com.a307.linkcare.feature.mypage.domain.repository.MyPageRepository
import com.a307.linkcare.feature.mypage.data.model.response.GroupCharacterResponse
import com.a307.linkcare.feature.caregroup.domain.repository.CareGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val careGroupRepository: CareGroupRepository,
    private val repo: MyPageRepository
) : ViewModel() {

    private val _apiState = MutableStateFlow<MyPageApiState>(MyPageApiState.Loading)
    val apiState: StateFlow<MyPageApiState> = _apiState

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing


    init {
        fetchMyPageData()
    }

    fun fetchMyPageData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isRefreshing.value = true
            } else {
                _apiState.value = MyPageApiState.Loading
            }
            try {
                coroutineScope {
                    val userResponseDeferred = async { RetrofitClient.userApi.getMyInfo() }
                    val healthResponseDeferred = async { careGroupRepository.getMyDailyHealthDetail() }

                    val userResponse = userResponseDeferred.await()
                    val healthData = healthResponseDeferred.await()

                    if (userResponse.isSuccessful && userResponse.body() != null) {
                        val userData = userResponse.body()!!

                        val uiState = MyPageUiState(
                            nickname = userData.nickname,
                            coinLabel = "보유 코인 ${userData.points}",
                            groupCountLabel = "그룹 조회",
                            avatarUrl = userData.mainCharacterImageUrl,
                            avatarBgUrl = userData.mainBackgroundImageUrl,
                            healthToday = healthData
                        )
                        _apiState.value = MyPageApiState.Success(uiState)
                    } else {
                        val errorMsg = "사용자 정보를 불러오지 못했습니다: ${userResponse.code()}"
                        Log.e("MyPageViewModel", errorMsg)
                        _apiState.value = MyPageApiState.Error(errorMsg)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "서버 통신 오류: ${e.message}"
                Log.e("MyPageViewModel", errorMsg, e)
                _apiState.value = MyPageApiState.Error(errorMsg)
            } finally {
                if (isRefresh) {
                    _isRefreshing.value = false
                }
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
