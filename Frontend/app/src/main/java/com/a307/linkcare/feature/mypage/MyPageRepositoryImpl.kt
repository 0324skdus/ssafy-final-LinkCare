package com.a307.linkcare.feature.mypage

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
}