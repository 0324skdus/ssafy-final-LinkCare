package com.a307.linkcare.feature.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.a307.linkcare.feature.notification.data.MyFirebaseMessagingService
import com.a307.linkcare.feature.notification.data.NotificationApi
import com.a307.linkcare.feature.notification.data.NotificationEventManager
import com.a307.linkcare.feature.notification.data.NotificationRepository
import com.a307.linkcare.feature.notification.data.ProcessedNotificationStore
import com.a307.linkcare.feature.notification.domain.AlarmResponse
import com.a307.linkcare.feature.notification.domain.NotificationResponse
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
    private val groupRepository: com.a307.linkcare.feature.commongroup.data.GroupRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val processedStore = ProcessedNotificationStore(context)

    // 그룹 이미지 캐시 (groupSeq -> imageUrl)
    private val _groupImages = MutableStateFlow<Map<Long, String>>(emptyMap())
    val groupImages: StateFlow<Map<Long, String>> = _groupImages

    // FCM 브로드캐스트 리시버 (백업용)
    private val fcmReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("NotificationViewModel", "브로드캐스트 수신: ${intent?.action}")
            // 새 알림이 왔을 때 자동으로 목록 갱신 (silent)
            loadNotifications("GROUP", silent = true)
        }
    }

    init {
        Log.d("NotificationViewModel", "ViewModel 초기화 시작")

        // ViewModel 생성 시 리시버 등록 (백업)
        registerFcmReceiver()

        // SharedFlow 이벤트 구독 (메인 방식)
        viewModelScope.launch {
            Log.d("NotificationViewModel", "SharedFlow 구독 시작")
            notificationEventManager.newNotificationEvent.collect {
                Log.d("NotificationViewModel", "✅ SharedFlow 이벤트 수신! - 알림 목록 갱신 호출")
                loadNotifications("GROUP", silent = true)
            }
        }

        Log.d("NotificationViewModel", "ViewModel 초기화 완료")
    }

    private val _notifications = MutableStateFlow<List<NotificationResponse>>(emptyList())
    val notifications: StateFlow<List<NotificationResponse>> = _notifications

    // 알림 (Alarms) - POKE와 LETTER
    private val _alarms = MutableStateFlow<List<AlarmResponse>>(emptyList())
    val alarms: StateFlow<List<AlarmResponse>> = _alarms

    // 콕 찌르기 (POKE만 필터링)
    private val _pokeAlarms = MutableStateFlow<List<AlarmResponse>>(emptyList())
    val pokeAlarms: StateFlow<List<AlarmResponse>> = _pokeAlarms

    // 편지 (LETTER만 필터링)
    private val _letterAlarms = MutableStateFlow<List<AlarmResponse>>(emptyList())
    val letterAlarms: StateFlow<List<AlarmResponse>> = _letterAlarms

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // 처리된 알림 정보 (DataStore에서 로드)
    val processedNotifications: StateFlow<Map<Long, String>> = processedStore.processedNotifications
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // 알림 목록 로드
    fun loadNotifications(category: String = "GROUP", silent: Boolean = false) {
        viewModelScope.launch {
            Log.d("NotificationViewModel", "loadNotifications 시작 (silent=$silent, category=$category)")

            if (!silent) {
                _isLoading.value = true
            }
            _errorMessage.value = null

            repository.getMyNotifications(category)
                .onSuccess { notifications ->
                    Log.d("NotificationViewModel", "✅ API 성공: ${notifications.size}개 알림 받음")
                    _notifications.value = notifications
                    Log.d("NotificationViewModel", "StateFlow 업데이트 완료")

                    // 그룹 이미지 로드
                    loadGroupImages(notifications)
                }
                .onFailure { error ->
                    Log.e("NotificationViewModel", "❌ API 실패: ${error.message}", error)
                    if (!silent) {
                        _errorMessage.value = error.message ?: "알림을 불러오는데 실패했습니다"
                    }
                }

            if (!silent) {
                _isLoading.value = false
            }

            Log.d("NotificationViewModel", "loadNotifications 완료")
        }
    }

    // 안 읽은 알림 로드 (POKE, LETTER)
    fun loadUnreadAlarms() {
        viewModelScope.launch {
            Log.d("NotificationViewModel", "[LOAD_ALARMS] 안 읽은 알림 조회 시작")

            try {
                val response = notificationApi.getUnreadAlarms()

                if (response.isSuccessful && response.body() != null) {
                    val allAlarms: List<AlarmResponse> = response.body()!!
                    _alarms.value = allAlarms

                    // messageType에 따라 분리
                    val pokes: List<AlarmResponse> = allAlarms.filter { it.messageType == "POKE" }
                    val letters: List<AlarmResponse> = allAlarms.filter { it.messageType == "LETTER" }

                    _pokeAlarms.value = pokes
                    _letterAlarms.value = letters

                    Log.d("NotificationViewModel", "[LOAD_ALARMS] ✅ 성공: 전체 ${allAlarms.size}개")
                    Log.d("NotificationViewModel", "[LOAD_ALARMS]   - 콕 찌르기: ${pokes.size}개")
                    Log.d("NotificationViewModel", "[LOAD_ALARMS]   - 편지: ${letters.size}개")
                } else {
                    Log.e("NotificationViewModel", "[LOAD_ALARMS] ❌ 실패: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "[LOAD_ALARMS] ❌ 예외", e)
            }
        }
    }

    // 특정 알림 읽음 처리 (Alarm용)
    fun markAlarmAsRead(alarmId: Long) {
        viewModelScope.launch {
            Log.d("NotificationViewModel", "[MARK_READ] 알림 읽음 처리 시작: alarmId=$alarmId")

            try {
                // 1. 서버에 읽음 처리 요청
                val response = notificationApi.markAlarmAsRead(alarmId)

                if (response.isSuccessful) {
                    Log.d("NotificationViewModel", "[MARK_READ] ✅ 서버 읽음 처리 성공")

                    // 2. 로컬 상태 업데이트 (흐림 처리)
                    val updated: List<AlarmResponse> = _alarms.value.map {
                        if (it.alarmId == alarmId) it.copy(read = true) else it
                    }
                    _alarms.value = updated

                    // 3. 다시 분리
                    val pokes: List<AlarmResponse> = updated.filter { it.messageType == "POKE" }
                    val letters: List<AlarmResponse> = updated.filter { it.messageType == "LETTER" }
                    _pokeAlarms.value = pokes
                    _letterAlarms.value = letters

                    Log.d("NotificationViewModel", "[MARK_READ] 로컬 상태 업데이트 완료")
                } else {
                    Log.e("NotificationViewModel", "[MARK_READ] ❌ 서버 읽음 처리 실패: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "[MARK_READ] ❌ 예외 발생", e)
            }
        }
    }

    // 전체 알림 읽음 처리 (Alarm용)
    fun markAllAlarmsAsRead() {
        viewModelScope.launch {
            Log.d("NotificationViewModel", "[MARK_ALL_READ] 전체 알림 읽음 처리 시작")

            try {
                // 안 읽은 알림들의 ID 수집
                val unreadAlarms: List<AlarmResponse> = _alarms.value.filter { !it.read }
                Log.d("NotificationViewModel", "[MARK_ALL_READ] 안 읽은 알림 ${unreadAlarms.size}개")

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

                Log.d("NotificationViewModel", "[MARK_ALL_READ] 처리 완료: $successCount/${unreadAlarms.size}개 성공")

                // 로컬 상태 업데이트 (모두 읽음으로 변경)
                val updated: List<AlarmResponse> = _alarms.value.map { it.copy(read = true) }
                _alarms.value = updated

                val pokes: List<AlarmResponse> = updated.filter { it.messageType == "POKE" }
                val letters: List<AlarmResponse> = updated.filter { it.messageType == "LETTER" }
                _pokeAlarms.value = pokes
                _letterAlarms.value = letters

            } catch (e: Exception) {
                Log.e("NotificationViewModel", "[MARK_ALL_READ] ❌ 전체 처리 예외", e)
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
            // 처리 결과 메시지 저장 (DataStore에 영구 저장)
            val message = "${userName}님 신청을 수락하였습니다"
            processedStore.addProcessedNotification(notificationId, message)
        }
        return result
    }

    // 가입 요청 거절
    suspend fun rejectJoinRequest(requestSeq: Long, notificationId: Long, userName: String): Result<Unit> {
        val result = repository.rejectJoinRequest(requestSeq)
        if (result.isSuccess) {
            // 처리 결과 메시지 저장 (DataStore에 영구 저장)
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
                Log.d("NotificationViewModel", "✅ 알림 삭제 성공: $notificationId")
                // 로컬 상태에서 제거
                _notifications.value = _notifications.value.filter {
                    it.notificationId != notificationId
                }
                // DataStore에서도 제거
                processedStore.removeProcessedNotification(notificationId)
            }.onFailure { error ->
                Log.e("NotificationViewModel", "❌ 알림 삭제 실패: ${error.message}", error)
                _errorMessage.value = error.message ?: "알림 삭제에 실패했습니다"
            }
            result
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "❌ 알림 삭제 예외: ${e.message}", e)
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
                // 이미 캐시에 있으면 스킵
                if (_groupImages.value.containsKey(groupSeq)) {
                    newImages[groupSeq] = _groupImages.value[groupSeq]!!
                } else {
                    // 그룹 상세 정보 가져오기
                    try {
                        val detail = groupRepository.getGroupDetail(groupSeq)
                        newImages[groupSeq] = detail.imageUrl
                        Log.d("NotificationViewModel", "그룹 이미지 로드 성공: $groupSeq -> ${detail.imageUrl}")
                    } catch (error: Exception) {
                        Log.e("NotificationViewModel", "그룹 이미지 로드 실패: $groupSeq - ${error.message}")
                    }
                }
            }

            // 캐시 업데이트
            _groupImages.value = _groupImages.value + newImages
        }
    }

    // FCM 리시버 등록
    private fun registerFcmReceiver() {
        val filter = IntentFilter().apply {
            addAction(MyFirebaseMessagingService.ACTION_GROUP_JOINED)
            addAction(MyFirebaseMessagingService.ACTION_NEW_NOTIFICATION)
        }

        try {
            // LocalBroadcastManager로 등록 (모든 버전)
            LocalBroadcastManager.getInstance(context).registerReceiver(fcmReceiver, filter)
            Log.d("NotificationViewModel", "LocalBroadcastManager 리시버 등록 완료")

            // Android 13+ 에서는 글로벌 리시버도 등록
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(fcmReceiver, filter, Context.RECEIVER_EXPORTED)
                Log.d("NotificationViewModel", "Global 리시버 등록 완료")
            }
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "리시버 등록 실패: ${e.message}", e)
        }
    }

    override fun onCleared() {
        super.onCleared()
        // ViewModel 소멸 시 리시버 등록 해제
        try {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(fcmReceiver)
            Log.d("NotificationViewModel", "LocalBroadcastManager 리시버 해제")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.unregisterReceiver(fcmReceiver)
                Log.d("NotificationViewModel", "Global 리시버 해제")
            }
        } catch (e: Exception) {
            Log.e("NotificationViewModel", "리시버 해제 실패: ${e.message}", e)
        }
    }
}
