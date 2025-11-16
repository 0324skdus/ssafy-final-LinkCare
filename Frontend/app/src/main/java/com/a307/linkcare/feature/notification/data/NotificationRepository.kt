package com.a307.linkcare.feature.notification.data

import com.a307.linkcare.feature.commongroup.data.GroupApi
import com.a307.linkcare.feature.notification.domain.NotificationResponse
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val notificationApi: NotificationApi,
    private val groupApi: GroupApi
) {

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

    // 가입 요청 승인
    suspend fun approveJoinRequest(requestSeq: Long): Result<Unit> {
        return try {
            val response = groupApi.approveJoinRequest(requestSeq)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to approve: HTTP ${response.code()}"))
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
                Result.failure(Exception("Failed to reject: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
