package com.a307.linkcare.feature.auth

import android.util.Log
import com.a307.linkcare.feature.auth.data.ProfileApi
import com.a307.linkcare.feature.auth.domain.UpdateProfileRequest
import javax.inject.Inject


class ProfileRepository @Inject constructor(
    private val api: ProfileApi
) {
    suspend fun updateProfile(
        birth: String,
        height: Float?,
        weight: Float?,
        gender: String,
        exerciseStartYear: Int?
    ) {
        val body = UpdateProfileRequest(
            birth = birth,
            height = height,
            weight = weight,
            gender = gender,
            exerciseStartYear = exerciseStartYear
        )

        Log.d("ProfileRepository", "updateProfile() 시작: $body")
        val res = api.updateProfile(body)
        Log.d("ProfileRepository", "응답  ${res.code()} / 성공 여부: ${res.isSuccessful}")

        if (!res.isSuccessful) {
            throw Exception("프로필 업데이트 실패: ${res.code()}")
        }
    }
}
