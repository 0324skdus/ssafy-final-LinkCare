package com.a307.linkcare.feature.mypage.data.model.response

data class GroupCharacterResponse(
    val userId: Long,
    val userName: String,
    val petName: String,
    val leader: Boolean,
    val mainCharacterImageUrl: String,
    val mainBackgroundImageUrl: String
)
