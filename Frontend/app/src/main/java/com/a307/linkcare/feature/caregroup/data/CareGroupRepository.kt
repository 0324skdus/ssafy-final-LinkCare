package com.a307.linkcare.feature.caregroup.data

import com.a307.linkcare.feature.auth.data.TokenStore
import okhttp3.MultipartBody
import javax.inject.Inject

class CareGroupRepository @Inject constructor(
    private val api: CareGroupApi,
    private val tokenStore: TokenStore
) {

    suspend fun createCareGroup(
        name: String,
        description: String,
        capacity: Int,
        imagePart: MultipartBody.Part?,
        isSleepAllowed: Boolean?,
        isWaterIntakeAllowed: Boolean?,
        isBloodPressureAllowed: Boolean?,
        isBloodSugarAllowed: Boolean?
    ): Result<Unit> {
        return try {
            val token = tokenStore.getAccess() ?: return Result.failure(Exception("No token"))

            val res = api.createCareGroup(
                token = "Bearer $token",
                groupName = name,
                description = description,
                capacity = capacity,
                image = imagePart,
                isSleepAllowed = isSleepAllowed,
                isWaterIntakeAllowed = isWaterIntakeAllowed,
                isBloodPressureAllowed = isBloodPressureAllowed,
                isBloodSugarAllowed = isBloodSugarAllowed
            )

            if (res.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("HTTP ${res.code()}"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
