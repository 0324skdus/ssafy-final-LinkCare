package com.a307.linkcare.feature.healthgroup.data

import com.a307.linkcare.feature.auth.data.TokenStore
import okhttp3.MultipartBody
import javax.inject.Inject

class HealthGroupRepository @Inject constructor(
    private val api: HealthGroupApi,
    private val tokenStore: TokenStore
) {

    suspend fun createHealthGroup(
        name: String,
        description: String,
        capacity: Int,
        minCalorie: Float?,
        minStep: Int?,
        minDistance: Float?,
        minDuration: Int?,
        imagePart: MultipartBody.Part?
    ): Result<Unit> {
        return try {
            val token = tokenStore.getAccess() ?: return Result.failure(Exception("No token"))

            val res = api.createHealthGroup(
                token = "Bearer $token",
                groupName = name,
                description = description,
                capacity = capacity,
                minCalorie = minCalorie,
                minStep = minStep,
                minDistance = minDistance,
                minDuration = minDuration,
                image = imagePart
            )

            if (res.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("HTTP ${res.code()}"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
