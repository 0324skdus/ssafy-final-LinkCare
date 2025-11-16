package com.a307.linkcare.feature.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.notification.data.NotificationRepository
import com.a307.linkcare.feature.notification.domain.NotificationResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _notifications = MutableStateFlow<List<NotificationResponse>>(emptyList())
    val notifications: StateFlow<List<NotificationResponse>> = _notifications

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // 알림 목록 로드
    fun loadNotifications(category: String = "GROUP") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            repository.getMyNotifications(category)
                .onSuccess { notifications ->
                    _notifications.value = notifications
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "알림을 불러오는데 실패했습니다"
                }

            _isLoading.value = false
        }
    }

    // 알림 읽음 처리
    fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            repository.markAsRead(notificationId)
                .onSuccess {
                    // 로컬 상태 업데이트
                    _notifications.value = _notifications.value.map {
                        if (it.notificationId == notificationId) {
                            it.copy(isRead = true)
                        } else {
                            it
                        }
                    }
                }
                .onFailure { error ->
                    _errorMessage.value = error.message
                }
        }
    }

    // 전체 읽음 처리
    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
                .onSuccess {
                    // 로컬 상태 업데이트
                    _notifications.value = _notifications.value.map {
                        it.copy(isRead = true)
                    }
                }
                .onFailure { error ->
                    _errorMessage.value = error.message
                }
        }
    }

    // 가입 요청 승인
    suspend fun approveJoinRequest(requestSeq: Long): Result<Unit> {
        return repository.approveJoinRequest(requestSeq)
    }

    // 가입 요청 거절
    suspend fun rejectJoinRequest(requestSeq: Long): Result<Unit> {
        return repository.rejectJoinRequest(requestSeq)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
