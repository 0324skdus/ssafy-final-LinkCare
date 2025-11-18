package com.a307.linkcare.feature.mypage

data class GroupDetailResponse(
    val groupId: Long,
    val groupName: String,
    val members: List<GroupCharacterResponse>
)
