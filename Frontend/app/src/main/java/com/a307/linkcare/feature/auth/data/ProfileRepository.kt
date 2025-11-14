package com.a307.linkcare.feature.auth.data

import android.util.Log
import com.a307.linkcare.feature.auth.domain.UpdateCharacterRequest
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