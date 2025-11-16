package com.a307.linkcare.feature.notification.data

import com.a307.linkcare.feature.notification.domain.NotificationResponse
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApi {

    // 내 알림 목록 조회
    @GET("/api/notifications")
    suspend fun getMyNotifications(
        @Query("category") category: String = "ALL"  // ALL, GROUP
    ): Response<List<NotificationResponse>>

    // 알림 읽음 처리
    @PATCH("/api/notifications/{notificationId}/read")
    suspend fun markAsRead(
        @Path("notificationId") notificationId: Long
    ): Response<Unit>

    // 전체 알림 읽음 처리
    @PATCH("/api/notifications/read-all")
    suspend fun markAllAsRead(): Response<Unit>

    // 알림 삭제
    @DELETE("/api/notifications/{notificationId}")
    suspend fun deleteNotification(
        @Path("notificationId") notificationId: Long
    ): Response<Unit>
}
