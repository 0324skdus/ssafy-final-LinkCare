package com.a307.linkcare.feature.notification.data.api

import com.a307.linkcare.feature.notification.domain.model.response.AlarmResponse
import com.a307.linkcare.feature.notification.domain.model.response.NotificationResponse
import com.a307.linkcare.feature.notification.domain.model.request.SaveNotificationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
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

    // 알림 저장 (콕 찌르기)
    @POST("/api/alarms/save")
    suspend fun saveNotification(
        @Body request: SaveNotificationRequest
    ): Response<Unit>

    // 모든 알림 조회 (삭제되지 않은 알림)
    @GET("/api/alarms/all")
    suspend fun getAllAlarms(): Response<List<AlarmResponse>>

    // 알림 읽음 처리
    @PATCH("/api/alarms/{alarmId}/read")
    suspend fun markAlarmAsRead(
        @Path("alarmId") alarmId: Long
    ): Response<Unit>

    // 알림 삭제
    @DELETE("/api/alarms/{alarmId}")
    suspend fun deleteAlarm(
        @Path("alarmId") alarmId: Long
    ): Response<Unit>
}