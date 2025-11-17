package com.a307.linkcare.feature.mypage


import com.a307.linkcare.feature.mypage.MyProfileResponse
import com.a307.linkcare.feature.mypage.UpdateProfileRequest

interface MyPageRepository {
    suspend fun getMyProfile(): MyProfileResponse
    suspend fun updateProfile(req: UpdateProfileRequest)
}