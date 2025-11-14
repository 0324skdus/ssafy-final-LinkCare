package com.a307.linkcare.feature.commongroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.commongroup.data.GroupRepository
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
}

