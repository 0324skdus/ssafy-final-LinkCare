package com.a307.linkcare.feature.commongroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.auth.data.TokenStore
import com.a307.linkcare.feature.commongroup.data.GroupRepository
import com.a307.linkcare.feature.commongroup.domain.GroupDetailResponse
import com.a307.linkcare.feature.commongroup.domain.MyGroupResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted


@HiltViewModel
class MyGroupsViewModel @Inject constructor(
    private val repo: GroupRepository,
    private val tokenStore: TokenStore
) : ViewModel() {

    val myUserSeq: Long? = tokenStore.getUserPk()

    private val _careGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val careGroups: StateFlow<List<MyGroupResponse>> = _careGroups

    private val _healthGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val healthGroups: StateFlow<List<MyGroupResponse>> = _healthGroups

    fun loadCare() {
        viewModelScope.launch {
            _careGroups.value = repo.getMyGroups("CARE")
        }
    }

    fun loadHealth() {
        viewModelScope.launch {
            _healthGroups.value = repo.getMyGroups("HEALTH")
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
}

