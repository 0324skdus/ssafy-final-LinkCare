package com.a307.linkcare.feature.mypage

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.auth.data.RetrofitClient
import com.a307.linkcare.feature.caregroup.data.CareGroupRepository
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
    private val careGroupRepository: CareGroupRepository
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
            }
        }
    }
}
