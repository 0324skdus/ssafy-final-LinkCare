package com.a307.linkcare.feature.mypage.data

import com.a307.linkcare.feature.mypage.domain.GroupDetailResponse
import com.a307.linkcare.feature.mypage.domain.MyProfileResponse
import com.a307.linkcare.feature.mypage.domain.UpdateProfileRequest
import javax.inject.Inject

class MyPageRepositoryImpl @Inject constructor(
    private val api: MyPageApi
) : MyPageRepository {

    override suspend fun getMyProfile(): MyProfileResponse {
        return api.getMyProfile()
    }

    override suspend fun updateProfile(req: UpdateProfileRequest) {
        api.updateProfile(req)
    }

    override suspend fun getMyGroupCharacters(groupSeq: Long): GroupDetailResponse {
        return api.getMyGroupCharacters(groupSeq)
    }
}