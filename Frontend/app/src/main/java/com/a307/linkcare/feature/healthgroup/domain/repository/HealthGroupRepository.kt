package com.a307.linkcare.feature.healthgroup.domain.repository

import android.util.Log
import com.a307.linkcare.common.network.store.TokenStore
import com.a307.linkcare.feature.healthgroup.data.api.HealthGroupApi
import com.a307.linkcare.feature.healthgroup.data.model.request.ActualActivity
import com.a307.linkcare.feature.healthgroup.data.model.response.DailyActivitySummaryResponse
import com.a307.linkcare.feature.healthgroup.data.model.request.UpdateGoalRequest
import com.a307.linkcare.feature.healthgroup.data.model.response.WeeklyGroupGoalResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
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
                groupName = name.toRequestBody("text/plain".toMediaTypeOrNull()),
                description = description.toRequestBody("text/plain".toMediaTypeOrNull()),
                capacity = capacity.toString().toRequestBody("text/plain".toMediaTypeOrNull()),
                minCalorie = minCalorie?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                minStep = minStep?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                minDistance = minDistance?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                minDuration = minDuration?.toString()?.toRequestBody("text/plain".toMediaTypeOrNull()),
                image = imagePart
            )

            if (res.isSuccessful) Result.success(Unit)
            else Result.failure(Exception("HTTP ${res.code()}"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateGroupGoals(
        groupSeq: Long,
        requestDate: String
    ): Result<WeeklyGroupGoalResponse> {
        return try {
            Log.d("HealthGroupRepository", "🚀 AI 목표 생성 요청 시작")
            Log.d("HealthGroupRepository", "  - groupSeq: $groupSeq")
            Log.d("HealthGroupRepository", "  - requestDate: $requestDate")

            val token = tokenStore.getAccess()
            if (token == null) {
                Log.e("HealthGroupRepository", "❌ 토큰이 없습니다")
                return Result.failure(Exception("No access token"))
            }
            Log.d("HealthGroupRepository", "  - token: ${token.take(20)}...")

            val response = api.generateGroupGoals(
                token = "Bearer $token",
                groupSeq = groupSeq,
                requestDate = requestDate
            )

            Log.d("HealthGroupRepository", "📡 API 응답 수신")
            Log.d("HealthGroupRepository", "  - HTTP 코드: ${response.code()}")
            Log.d("HealthGroupRepository", "  - 성공 여부: ${response.isSuccessful}")

            if (response.isSuccessful && response.body() != null) {
                val goals = response.body()!!
                Log.d("HealthGroupRepository", "✅ AI 목표 생성 성공!")
                Log.d("HealthGroupRepository", "  - weekStart: ${goals.weekStart}")
                Log.d("HealthGroupRepository", "  - goalSteps: ${goals.goalSteps}")
                Log.d("HealthGroupRepository", "  - goalKcal: ${goals.goalKcal}")
                Log.d("HealthGroupRepository", "  - goalDuration: ${goals.goalDuration}")
                Log.d("HealthGroupRepository", "  - goalDistance: ${goals.goalDistance}")
                Log.d("HealthGroupRepository", "  - growthRateSteps: ${goals.predictedGrowthRateSteps}")
                Result.success(goals)
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("HealthGroupRepository", "❌ API 에러")
                Log.e("HealthGroupRepository", "  - 코드: ${response.code()}")
                Log.e("HealthGroupRepository", "  - 메시지: ${response.message()}")
                Log.e("HealthGroupRepository", "  - 에러 본문: $errorBody")
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("HealthGroupRepository", "❌ 예외 발생: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentGoals(groupSeq: Long): Result<WeeklyGroupGoalResponse?> {
        return try {
            Log.d("HealthGroupRepository", "📥 현재 주차 목표 조회")
            Log.d("HealthGroupRepository", "  - groupSeq: $groupSeq")

            val token = tokenStore.getAccess() ?: return Result.failure(Exception("No token"))

            val response = api.getCurrentGoals(
                token = "Bearer $token",
                groupSeq = groupSeq
            )

            if (response.isSuccessful && response.body() != null) {
                val goals = response.body()!!
                Log.d("HealthGroupRepository", "✅ 목표 조회 성공")
                Log.d("HealthGroupRepository", "  - selectedMetricType: ${goals.selectedMetricType}")
                Result.success(goals)
            } else if (response.code() == 404) {
                // 목표가 없는 경우
                Log.d("HealthGroupRepository", "💤 목표 없음 (404)")
                Result.success(null)
            } else {
                Log.e("HealthGroupRepository", "❌ 목표 조회 실패: ${response.code()}")
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception)
        {
            Log.e("HealthGroupRepository", "❌ 예외 발생: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateGoal(
        groupSeq: Long,
        metricType: String,
        goalValue: Long
    ): Result<WeeklyGroupGoalResponse> {
        return try {
            Log.d("HealthGroupRepository", "💾 목표 저장")
            Log.d("HealthGroupRepository", "  - groupSeq: $groupSeq")
            Log.d("HealthGroupRepository", "  - metricType: $metricType")
            Log.d("HealthGroupRepository", "  - goalValue: $goalValue")

            val token = tokenStore.getAccess() ?: return Result.failure(Exception("No token"))

            val request = UpdateGoalRequest(
                selectedMetricType = metricType,
                goalValue = goalValue
            )

            val response = api.updateGoal(
                token = "Bearer $token",
                groupSeq = groupSeq,
                request = request
            )

            if (response.isSuccessful && response.body() != null) {
                Log.d("HealthGroupRepository", "✅ 목표 저장 성공")
                Result.success(response.body()!!)
            } else {
                Log.e("HealthGroupRepository", "❌ 목표 저장 실패: ${response.code()}")
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e("HealthGroupRepository", "❌ 예외 발생: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getActualActivityForUser(
        userSeq: Int,
        date: String
    ): Result<ActualActivity> {
        return try {
            val token = tokenStore.getAccess() ?: return Result.failure(Exception("No token"))
            val response = api.getActualActivity(
                token = "Bearer $token",
                userSeq = userSeq,
                startDate = date,
                endDate = date
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActualActivityForUserRange(
        userSeq: Int,
        startDate: String,
        endDate: String
    ): Result<ActualActivity> {
        return try {
            val token = tokenStore.getAccess() ?: return Result.failure(Exception("No token"))
            val response = api.getActualActivity(
                token = "Bearer $token",
                userSeq = userSeq,
                startDate = startDate,
                endDate = endDate
            )

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else if (response.code() == 404) {
                // 데이터가 없는 경우, 0으로 채운 성공으로 처리
                Result.success(ActualActivity(0, 0.0, 0.0, 0))
            }
            else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDailyActivity(userSeq: Int, date: LocalDate): DailyActivitySummaryResponse? {
        return try {
            val response = api.getDailyActivity(userSeq, date)
            if (response.isSuccessful) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("HealthGroupRepository", "Error getting daily activity", e)
            null
        }
    }
}