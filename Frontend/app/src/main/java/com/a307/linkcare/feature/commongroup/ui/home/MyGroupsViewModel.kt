package com.a307.linkcare.feature.commongroup.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.common.network.store.TokenStore
import com.a307.linkcare.feature.commongroup.domain.repository.GroupRepository
import com.a307.linkcare.feature.commongroup.data.model.response.GroupDetailResponse
import com.a307.linkcare.feature.commongroup.data.model.request.GroupTotalActivityStats
import com.a307.linkcare.feature.commongroup.data.model.response.MyGroupResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.time.LocalDate


@HiltViewModel
class MyGroupsViewModel @Inject constructor(
    private val repo: GroupRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    val myUserSeq: Long? = tokenStore.getUserPk()

    // 그룹 목록 새로고침 이벤트
    private val _refreshEvent = MutableSharedFlow<Unit>(replay = 0)
    val refreshEvent = _refreshEvent.asSharedFlow()

    // 그룹 신청 후 호출되는 함수
    fun notifyGroupApplicationSubmitted() {
        viewModelScope.launch {
            Log.d("MyGroupsViewModel", "그룹 신청 이벤트 발행")
            _refreshEvent.emit(Unit)
        }
    }

    private val _careGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val careGroups: StateFlow<List<MyGroupResponse>> = _careGroups

    private val _healthGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val healthGroups: StateFlow<List<MyGroupResponse>> = _healthGroups

    private val _carePendingGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val carePendingGroups: StateFlow<List<MyGroupResponse>> = _carePendingGroups

    private val _healthPendingGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val healthPendingGroups: StateFlow<List<MyGroupResponse>> = _healthPendingGroups

    fun loadCare() {
        viewModelScope.launch {
            Log.d("MyGroupsViewModel", "케어 그룹 목록 로드 시작")
            val groups = repo.getMyGroups("CARE")
            _careGroups.value = groups
            Log.d("MyGroupsViewModel", "케어 그룹 목록 로드 완료: ${groups.size}개")
        }
    }

    fun loadHealth() {
        viewModelScope.launch {
            Log.d("MyGroupsViewModel", "헬스 그룹 목록 로드 시작")
            val groups = repo.getMyGroups("HEALTH")
            _healthGroups.value = groups
            Log.d("MyGroupsViewModel", "헬스 그룹 목록 로드 완료: ${groups.size}개")
        }
    }

    fun loadCarePending() {
        viewModelScope.launch {
            Log.d("MyGroupsViewModel", "케어 신청 그룹 목록 로드 시작")
            val groups = repo.getMyPendingGroups("CARE")
            _carePendingGroups.value = groups
            Log.d("MyGroupsViewModel", "케어 신청 그룹 목록 로드 완료: ${groups.size}개")
        }
    }

    fun loadHealthPending() {
        viewModelScope.launch {
            Log.d("MyGroupsViewModel", "헬스 신청 그룹 목록 로드 시작")
            val groups = repo.getMyPendingGroups("HEALTH")
            _healthPendingGroups.value = groups
            Log.d("MyGroupsViewModel", "헬스 신청 그룹 목록 로드 완료: ${groups.size}개")
        }
    }

    private val _detail = MutableStateFlow<GroupDetailResponse?>(null)
    val detail: StateFlow<GroupDetailResponse?> = _detail

    fun loadGroupDetail(groupSeq: Long) {
        viewModelScope.launch {
            _detail.value = repo.getGroupDetail(groupSeq)
        }
    }

    val members = detail
        .map { it?.members ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val groupName = detail
        .map { it?.groupName ?: "" }
        .stateIn(viewModelScope, SharingStarted.Eagerly, "")

    // 초대 링크 생성
    suspend fun createInvitationLink(groupSeq: Long): Result<String> {
        return try {
            val result = repo.createInvitation(groupSeq)
            result.map { invitation ->
                // 커스텀 스킴 사용 (앱에서만 열림)
                "linkcare://invite/${invitation.invitationToken}"
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 그룹 이번주 운동 통계
    private val _totalActivityStats =
        MutableStateFlow<GroupTotalActivityStats?>(null)
    val totalActivityStats: StateFlow<GroupTotalActivityStats?> =
        _totalActivityStats.asStateFlow()

    fun loadTotalActivityStats(
        groupSeq: Long,
        startDate: LocalDate,
        endDate: LocalDate
    ) {
        viewModelScope.launch {
            runCatching {
                repo.getTotalActivityStats(
                    groupSeq, startDate, endDate
                )
            }.onSuccess {
                _totalActivityStats.value = it
            }.onFailure {
                _totalActivityStats.value = null
            }
        }
    }
}

