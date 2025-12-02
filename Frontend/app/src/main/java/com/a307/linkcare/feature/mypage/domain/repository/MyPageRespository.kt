package com.a307.linkcare.feature.mypage.domain.repository


import com.a307.linkcare.feature.mypage.data.model.response.GroupDetailResponse
import com.a307.linkcare.feature.mypage.data.model.response.MyProfileResponse
import com.a307.linkcare.feature.mypage.data.model.request.UpdateProfileRequest

interface MyPageRepository {
    suspend fun getMyProfile(): MyProfileResponse
    suspend fun updateProfile(req: UpdateProfileRequest)
    suspend fun getMyGroupCharacters(groupSeq: Long): GroupDetailResponse
}