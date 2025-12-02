package com.a307.linkcare.feature.notification.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.a307.linkcare.feature.commongroup.domain.repository.GroupRepository
import com.a307.linkcare.feature.notification.service.MyFirebaseMessagingService
import com.a307.linkcare.feature.notification.data.api.NotificationApi
import com.a307.linkcare.feature.notification.manager.NotificationEventManager
import com.a307.linkcare.feature.notification.domain.repository.NotificationRepository
import com.a307.linkcare.common.network.store.ProcessedNotificationStore
import com.a307.linkcare.feature.notification.domain.model.response.AlarmResponse
import com.a307.linkcare.feature.notification.domain.model.response.NotificationResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val notificationApi: NotificationApi,
    private val notificationEventManager: NotificationEventManager,
    private val groupRepository: GroupRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val processedStore = ProcessedNotificationStore(context)

    private val _notifications = MutableStateFlow<List<NotificationResponse>>(emptyList())
    val notifications: StateFlow<List<NotificationResponse>> = _notifications

    private val _alarms = MutableStateFlow<List<AlarmResponse>>(emptyList())
    val alarms: StateFlow<List<AlarmResponse>> = _alarms

    private val _pokeAlarms = MutableStateFlow<List<AlarmResponse>>(emptyList())
    val pokeAlarms: StateFlow<List<AlarmResponse>> = _pokeAlarms

    private val _letterAlarms = MutableStateFlow<List<AlarmResponse>>(emptyList())
    val letterAlarms: StateFlow<List<AlarmResponse>> = _letterAlarms

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _groupImages = MutableStateFlow<Map<Long, String>>(emptyMap())
    val groupImages: StateFlow<Map<Long, String>> = _groupImages

    val processedNotifications: StateFlow<Map<Long, String>> = processedStore.processedNotifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val fcmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadNotifications("GROUP", silent = true)
        }
    }

    init {
        registerFcmReceiver()

        viewModelScope.launch {
            notificationEventManager.newNotificationEvent.collect {
                loadNotifications("GROUP", silent = true)
            }
        }
    }

    // 알림 목록 로드
    fun loadNotifications(category: String = "GROUP", silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) {
                _isLoading.value = true
            }
            _isRefreshing.value = true
            _errorMessage.value = null

            repository.getMyNotifications(category)
                .onSuccess { notifications ->
                    _notifications.value = notifications
                    loadGroupImages(notifications)
                }
                .onFailure { error ->
                    if (!silent) {
                        _errorMessage.value = error.message ?: "알림을 불러오는데 실패했습니다"
                    }
                }

            if (!silent) {
                _isLoading.value = false
            }
            _isRefreshing.value = false
        }
    }

    // 모든 알림 로드
    fun loadAllAlarms() {
        viewModelScope.launch {
            _isRefreshing.value = true

            try {
                val response = notificationApi.getAllAlarms()

                if (response.isSuccessful && response.body() != null) {
                    val allAlarms: List<AlarmResponse> = response.body()!!
                    _alarms.value = allAlarms
                    val pokes = allAlarms.filter { it.messageType == "POKE" }
                    val letters = allAlarms.filter { it.messageType == "LETTER" }
                    _pokeAlarms.value = pokes
                    _letterAlarms.value = letters
                } else {
                }
            } catch (e: Exception) {
            }
            _isRefreshing.value = false
        }
    }

    // 특정 알림 읽음 처리
    fun markAlarmAsRead(alarmId: Long) {
        viewModelScope.launch {

            try {
                val response = notificationApi.markAlarmAsRead(alarmId)

                if (response.isSuccessful) {
                    val updated: List<AlarmResponse> = _alarms.value.map {
                        if (it.alarmId == alarmId) it.copy(read = true) else it
                    }
                    _alarms.value = updated

                    val pokes: List<AlarmResponse> = updated.filter { it.messageType == "POKE" }
                    val letters: List<AlarmResponse> = updated.filter { it.messageType == "LETTER" }
                    _pokeAlarms.value = pokes
                    _letterAlarms.value = letters
                } else {
                    Log.e("NotificationViewModel", "[MARK_READ] 서버 읽음 처리 실패: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "[MARK_READ] 예외 발생", e)
            }
        }
    }

    // 전체 알림 읽음 처리
    fun markAllAlarmsAsRead() {
        viewModelScope.launch {

            try {
                // 안 읽은 알림들의 ID 수집
                val unreadAlarms: List<AlarmResponse> = _alarms.value.filter { !it.read }

                // 각 알림에 대해 읽음 처리 API 호출
                var successCount = 0
                for (alarm in unreadAlarms) {
                    try {
                        val response = notificationApi.markAlarmAsRead(alarm.alarmId)
                        if (response.isSuccessful) {
                            successCount++
                        } else {
                            Log.e("NotificationViewModel", "[MARK_ALL_READ] 실패: alarmId=${alarm.alarmId}, HTTP ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationViewModel", "[MARK_ALL_READ] 예외: alarmId=${alarm.alarmId}", e)
                    }
                }

                // 로컬 상태 업데이트
                val updated: List<AlarmResponse> = _alarms.value.map { it.copy(read = true) }
                _alarms.value = updated

                val pokes: List<AlarmResponse> = updated.filter { it.messageType == "POKE" }
                val letters: List<AlarmResponse> = updated.filter { it.messageType == "LETTER" }
                _pokeAlarms.value = pokes
                _letterAlarms.value = letters

            } catch (e: Exception) {
                Log.e("NotificationViewModel", "[MARK_ALL_READ] 전체 처리 예외", e)
            }
        }
    }

    // 알림 삭제
    fun deleteAlarm(alarmId: Long) {
        viewModelScope.launch {
            try {
                val response = notificationApi.deleteAlarm(alarmId)

                if (response.isSuccessful) {
                    // 로컬 상태에서 제거
                    val updated: List<AlarmResponse> = _alarms.value.filter { it.alarmId != alarmId }
                    _alarms.value = updated

                    // 다시 분리
                    val pokes: List<AlarmResponse> = updated.filter { it.messageType == "POKE" }
                    val letters: List<AlarmResponse> = updated.filter { it.messageType == "LETTER" }
                    _pokeAlarms.value = pokes
                    _letterAlarms.value = letters

                } else {
                    Log.e("NotificationViewModel", "[DELETE_ALARM] 서버 삭제 실패: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "[DELETE_ALARM] 예외 발생", e)
            }
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
    suspend fun approveJoinRequest(requestSeq: Long, notificationId: Long, userName: String): Result<Unit> {
        val result = repository.approveJoinRequest(requestSeq)
        if (result.isSuccess) {
            // 처리 결과 메시지 저장
            val message = "${userName}님 신청을 수락하였습니다"
            processedStore.addProcessedNotification(notificationId, message)
        }
        return result
    }

    // 가입 요청 거절
    suspend fun rejectJoinRequest(requestSeq: Long, notificationId: Long, userName: String): Result<Unit> {
        val result = repository.rejectJoinRequest(requestSeq)
        if (result.isSuccess) {
            // 처리 결과 메시지 저장
            val message = "${userName}님 신청을 거절하였습니다"
            processedStore.addProcessedNotification(notificationId, message)
        }
        return result
    }

    // 알림 삭제
    suspend fun deleteNotification(notificationId: Long): Result<Unit> {
        return try {
            val result = repository.deleteNotification(notificationId)
            result.onSuccess {
                _notifications.value = _notifications.value.filter {
                    it.notificationId != notificationId
                }
                processedStore.removeProcessedNotification(notificationId)
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "알림 삭제에 실패했습니다"
            }
            result
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "알림 삭제 중 오류가 발생했습니다"
            Result.failure(e)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    // 그룹 이미지 로드
    private fun loadGroupImages(notifications: List<NotificationResponse>) {
        viewModelScope.launch {
            val groupSeqs = notifications.mapNotNull { it.relatedGroupSeq }.distinct()
            val newImages = mutableMapOf<Long, String>()

            groupSeqs.forEach { groupSeq ->
                if (!_groupImages.value.containsKey(groupSeq)) {
                    try {
                        val detail = groupRepository.getGroupDetail(groupSeq)
                        newImages[groupSeq] = detail.imageUrl
                    } catch (error: Exception) {
                        Log.e("NotificationViewModel", "그룹 이미지 로드 실패: $groupSeq - ${error.message}")
                    }
                }
            }

            if (newImages.isNotEmpty()) {
                _groupImages.value = _groupImages.value + newImages
            }
        }
    }

    // FCM 리시버 등록
    private fun registerFcmReceiver() {
        val filter = IntentFilter().apply {
            addAction(MyFirebaseMessagingService.ACTION_GROUP_JOINED)
            addAction(MyFirebaseMessagingService.ACTION_NEW_NOTIFICATION)
        }

        try {
            LocalBroadcastManager.getInstance(context).registerReceiver(fcmReceiver, filter)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(fcmReceiver, filter, Context.RECEIVER_EXPORTED)
            }
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "리시버 등록 실패: ${e.message}", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(fcmReceiver)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.unregisterReceiver(fcmReceiver)
            }
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "리시버 해제 실패: ${e.message}", e)
        }
    }
}
