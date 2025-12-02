package com.a307.linkcare.feature.auth.domain.repository

import android.util.Log
import com.a307.linkcare.feature.auth.data.api.ProfileApi
import com.a307.linkcare.feature.auth.data.model.request.UpdateCharacterRequest
import com.a307.linkcare.feature.auth.data.model.request.UpdateProfileRequest
import javax.inject.Inject

class ProfileRepository @Inject constructor(
    private val api: ProfileApi
) {
    suspend fun updateProfile(
        birth: String,
        height: Float?,
        weight: Float?,
        gender: String,
        exerciseStartYear: Int?,
        petName: String?
    ) {
        val body = UpdateProfileRequest(
            birth = birth,
            height = height,
            weight = weight,
            gender = gender,
            exerciseStartYear = exerciseStartYear,
            petName = petName
        )

        val res = api.updateProfile(body)

        if (!res.isSuccessful) {
            throw Exception("프로필 업데이트 실패: ${res.code()}")
        }
    }

    suspend fun selectInitial(
        characterId: Long,
        petName: String
    ) {
        val body = UpdateCharacterRequest(
            characterId = characterId,
            petName = petName
        )

        Log.d("ProfileRepository", "selectInitial body = $body")

        val res = api.selectInitial(characterId, petName)

        if (!res.isSuccessful) {
            throw Exception("프로필 - 캐릭터 업데이트 실패: ${res.code()}")
        }
    }
}