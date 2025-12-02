package com.a307.linkcare.feature.notification.domain.repository

import android.util.Log
import com.a307.linkcare.feature.commongroup.data.api.GroupApi
import com.a307.linkcare.feature.notification.data.api.NotificationApi
import com.a307.linkcare.feature.notification.domain.model.response.NotificationResponse
import com.google.gson.Gson
import com.google.gson.JsonObject
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val notificationApi: NotificationApi,
    private val groupApi: GroupApi
) {
    private val gson = Gson()

    // 알림 목록 조회
    suspend fun getMyNotifications(category: String = "ALL"): Result<List<NotificationResponse>> {
        return try {
            val response = notificationApi.getMyNotifications(category)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get notifications: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 알림 읽음 처리
    suspend fun markAsRead(notificationId: Long): Result<Unit> {
        return try {
            val response = notificationApi.markAsRead(notificationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark as read: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 전체 읽음 처리
    suspend fun markAllAsRead(): Result<Unit> {
        return try {
            val response = notificationApi.markAllAsRead()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark all as read: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 알림 삭제
    suspend fun deleteNotification(notificationId: Long): Result<Unit> {
        return try {
            val response = notificationApi.deleteNotification(notificationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                    ?: "알림 삭제 실패: HTTP ${response.code()}"
                Log.e("NotificationRepository", "삭제 실패: $errorMessage (HTTP ${response.code()})")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "삭제 예외: ${e.message}", e)
            Result.failure(e)
        }
    }

    // 가입 요청 승인
    suspend fun approveJoinRequest(requestSeq: Long): Result<Unit> {
        return try {
            val response = groupApi.approveJoinRequest(requestSeq)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 가입 요청 거절
    suspend fun rejectJoinRequest(requestSeq: Long): Result<Unit> {
        return try {
            val response = groupApi.rejectJoinRequest(requestSeq)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMessage = parseErrorMessage(response.errorBody()?.string())
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 서버 에러 응답에서 사용자 친화적인 메시지 추출
     * 예: {"timestamp":"2025-11-16T10:30:00","status":400,"error":"BAD_REQUEST","message":"이미 처리된 가입 신청입니다"}
     */
    private fun parseErrorMessage(errorBody: String?): String {
        return try {
            if (errorBody.isNullOrEmpty()) {
                return "요청 처리에 실패했습니다"
            }

            val jsonObject = gson.fromJson(errorBody, JsonObject::class.java)
            val message = jsonObject.get("message")?.asString

            // 서버에서 받은 메시지가 있으면 그대로 반환 (이미 한글로 작성됨)
            message ?: "요청 처리에 실패했습니다"
        } catch (e: Exception) {
            "요청 처리에 실패했습니다"
        }
    }
}