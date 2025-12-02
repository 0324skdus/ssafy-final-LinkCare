package com.a307.linkcare.feature.commongroup.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.commongroup.domain.repository.GroupRepository
import com.a307.linkcare.feature.commongroup.data.model.response.MyGroupResponse
import com.a307.linkcare.feature.commongroup.data.model.dto.PermissionAgreementDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupSearchViewModel @Inject constructor(
    private val repo: GroupRepository
) : ViewModel() {

    private val _searchResults = MutableStateFlow<List<MyGroupResponse>>(emptyList())
    val searchResults: StateFlow<List<MyGroupResponse>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun searchGroups(keyword: String) {
        if (keyword.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            repo.searchGroups(keyword)
                .onSuccess { groups ->
                    _searchResults.value = groups
                }
                .onFailure { error ->
                    _errorMessage.value = error.message ?: "검색 실패"
                    _searchResults.value = emptyList()
                }

            _isLoading.value = false
        }
    }

    fun loadAllGroups(type: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val groups = repo.getAllGroups(type)
            _searchResults.value = groups.map { groupResponse ->
                // GroupResponse를 MyGroupResponse로 변환
                MyGroupResponse(
                    groupSeq = groupResponse.groupSeq.toLong(),
                    groupName = groupResponse.groupName,
                    groupDescription = groupResponse.groupDescription,
                    currentMembers = groupResponse.currentMembers,
                    capacity = groupResponse.capacity,
                    imageUrl = groupResponse.imageUrl ?: "",
                    type = groupResponse.type,
                    createdAt = groupResponse.createdAt,
                    joinStatus = groupResponse.joinStatus
                )
            }

            _isLoading.value = false
        }
    }

    suspend fun joinGroup(groupSeq: Long, permissions: PermissionAgreementDto): Result<Unit> {
        return repo.joinGroupBySearch(groupSeq, permissions)
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
