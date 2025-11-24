package com.a307.linkcare.feature.mypage.data.repository

import com.a307.linkcare.feature.mypage.domain.repository.MyPageRepository
import com.a307.linkcare.feature.mypage.data.api.MyPageApi
import com.a307.linkcare.feature.mypage.data.model.response.GroupDetailResponse
import com.a307.linkcare.feature.mypage.data.model.response.MyProfileResponse
import com.a307.linkcare.feature.mypage.data.model.request.UpdateProfileRequest
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