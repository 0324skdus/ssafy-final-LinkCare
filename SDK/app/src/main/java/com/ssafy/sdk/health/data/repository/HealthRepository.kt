package com.ssafy.sdk.health.data.repository

import com.ssafy.sdk.health.data.model.ExerciseData
import com.ssafy.sdk.health.data.remote.ApiResponse
import com.ssafy.sdk.health.data.remote.HealthApiService
import com.ssafy.sdk.health.domain.sync.AllHealthData
import com.ssafy.sdk.health.domain.sync.DailyHealthData
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 건강 데이터 Repository
 * API 통신 및 에러 처리
 */
@Singleton
class HealthRepository @Inject constructor(
    private val apiService: HealthApiService
) {

    /**
     * 하루치 데이터 서버 전송
     */
    suspend fun uploadDailyHealthData(
        data: DailyHealthData
    ): Result<Unit> {
        return try {
            val response = apiService.uploadDailyHealthData(data)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        } as Result<Unit>
    }

    /**
     * 전체 데이터 서버 전송
     */
    suspend fun uploadAllHealthData(
        data: AllHealthData
    ): Result<Unit> {
        return try {
            val response = apiService.uploadAllHealthData(data)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        } as Result<Unit>
    }

    /**
     * 사용자별 하루치 데이터 전송
     */
    suspend fun uploadUserDailyHealthData(
        userId: Int,
        data: DailyHealthData
    ): Result<Unit> {
        return try {
            val response = apiService.uploadUserDailyHealthData(userId, data)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        } as Result<Unit>
    }

    /**
     * 사용자별 전체 데이터 전송
     */
    suspend fun uploadUserAllHealthData(
        userId: Int,
        data: AllHealthData
    ): Result<Unit> {
        return try {
            val response = apiService.uploadUserAllHealthData(userId, data)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        } as Result<Unit>
    }

    /**
     * 운동 데이터만 서버 전송
     */
    suspend fun uploadExerciseOnly(
        userId: Int,
        exercises: ExerciseData
    ): Result<Unit> {
        return try {
            val response = apiService.uploadExerciseOnly(userId, exercises)
            handleResponse(response)
        } catch (e: Exception) {
            Result.failure(e)
        } as Result<Unit>
    }

    /**
     * API 응답 처리
     */
    private fun <T> handleResponse(response: Response<ApiResponse<T>>): Result<T?> {
        return if (response.isSuccessful) {
            val body = response.body()
            if (body?.success == true) {
                Result.success(body.data)
            } else {
                Result.failure(Exception(body?.message ?: "Unknown error"))
            }
        } else {
            Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
        }
    }
}