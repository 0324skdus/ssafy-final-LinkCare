package com.a307.linkcare.feature.mypage.data.model.response

data class GroupDetailResponse(
    val groupId: Long,
    val groupName: String,
    val members: List<GroupCharacterResponse>
)
