package com.a307.linkcare.feature.caregroup.data.model.request

data class GroupStepUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val goal: Int = 0,              // totalSteps
    val progresses: List<Int> = emptyList(), // members.steps
    val members: List<MemberStep> = emptyList()
)