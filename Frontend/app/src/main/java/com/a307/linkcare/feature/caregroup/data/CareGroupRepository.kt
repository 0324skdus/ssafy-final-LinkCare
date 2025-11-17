package com.a307.linkcare.feature.caregroup.data

import com.a307.linkcare.feature.auth.data.TokenStore
import com.a307.linkcare.feature.caregroup.HealthToday
import com.a307.linkcare.feature.caregroup.domain.GroupStepStatisticsResponse
import com.a307.linkcare.feature.caregroup.domain.HealthSummaryResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import javax.inject.Inject

class CareGroupRepository @Inject constructor(
    private val api: CareGroupApi,
    private val healthFeedbackApi: HealthFeedbackApi,
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
                groupName = name.toRequestBody("text/plain".toMediaTypeOrNull()),
                description = description.toRequestBody("text/plain".toMediaTypeOrNull()),
                capacity = capacity.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                image = imagePart,
                isSleepAllowed = isSleepAllowed?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                isWaterIntakeAllowed = isWaterIntakeAllowed?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                isBloodPressureAllowed = isBloodPressureAllowed?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                isBloodSugarAllowed = isBloodSugarAllowed?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull())
            )

            if (res.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("HTTP ${res.code()}"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }



suspend fun getDailyHealthDetail(userSeq: Int): HealthToday? {
        return try {
            val response = api.getDailyHealthDetail(userSeq)
            if (response.isSuccessful) {
                response.body()?.toHealthToday()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchWeeklyHeader(groupSeq: Long, today: LocalDate?)
            = api.getWeeklyHeader(groupSeq, today)

    suspend fun fetchWeeklySleepStatistics(groupSeq: Long, startDate: LocalDate, endDate: LocalDate)
            = api.getWeeklySleepStatistics(groupSeq, startDate, endDate)

    suspend fun getGroupStepStatistics(groupSeq: Long): Result<GroupStepStatisticsResponse> {
        return try {
            val res = api.getGroupStepStatistics(groupSeq)
            if (res.isSuccessful) {
                val body = res.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(IllegalStateException("Empty body"))
                }
            } else {
                Result.failure(IllegalStateException("HTTP ${res.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHealthFeedback(userSeq: Int): HealthSummaryResponse? {
        return try {
            val response = healthFeedbackApi.getHealthFeedback(userSeq)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
