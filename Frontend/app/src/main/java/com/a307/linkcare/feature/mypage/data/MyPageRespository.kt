package com.a307.linkcare.feature.mypage.data


import com.a307.linkcare.feature.mypage.domain.GroupDetailResponse
import com.a307.linkcare.feature.mypage.domain.MyProfileResponse
import com.a307.linkcare.feature.mypage.domain.UpdateProfileRequest

interface MyPageRepository {
    suspend fun getMyProfile(): MyProfileResponse
    suspend fun updateProfile(req: UpdateProfileRequest)
    suspend fun getMyGroupCharacters(groupSeq: Long): GroupDetailResponse
}