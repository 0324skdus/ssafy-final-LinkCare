package com.a307.linkcare.feature.commongroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.commongroup.data.GroupRepository
import com.a307.linkcare.feature.commongroup.domain.GroupDetailResponse
import com.a307.linkcare.feature.commongroup.domain.MemberDto
import com.a307.linkcare.feature.commongroup.domain.MyGroupResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MyGroupsViewModel @Inject constructor(
    private val repo: GroupRepository
) : ViewModel() {

    private val _careGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val careGroups: StateFlow<List<MyGroupResponse>> = _careGroups

    private val _healthGroups = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val healthGroups: StateFlow<List<MyGroupResponse>> = _healthGroups

    private val _detail = MutableStateFlow<GroupDetailResponse?>(null)
    val detail: StateFlow<GroupDetailResponse?> = _detail

    private val _members = MutableStateFlow<List<MemberDto>>(emptyList())
    val members: StateFlow<List<MemberDto>> = _members

    private val _groupName = MutableStateFlow("")
    val groupName: StateFlow<String> = _groupName

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

    fun loadGroupDetail(groupSeq: Long) {
        viewModelScope.launch {
            try {
                val response = repo.getGroupDetail(groupSeq)
                _detail.value = response
                _members.value = response.members
                _groupName.value = response.groupName
            } catch (e: Exception) {
                e.printStackTrace()
                _detail.value = null
                _members.value = emptyList()
                _groupName.value = ""
            }
        }
    }
}

