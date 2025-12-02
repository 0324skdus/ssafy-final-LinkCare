package com.a307.linkcare.feature.notification.domain.model.response

import com.google.gson.annotations.SerializedName

data class NotificationResponse(
    @SerializedName("notificationId") val notificationId: Long,
    @SerializedName("type") val type: String,  // NotificationType enum
    @SerializedName("title") val title: String,  // 그룹 이름
    @SerializedName("content") val content: String,  // 알림 내용
    @SerializedName("relatedGroupSeq") val relatedGroupSeq: Long?,  // 관련 그룹 ID
    @SerializedName("relatedRequestSeq") val relatedRequestSeq: Long?,  // 관련 참가 신청 ID
    @SerializedName("isRead") val isRead: Boolean,
    @SerializedName("createdAt") val createdAt: String  // LocalDateTime -> String
)

// NotificationType enum
object NotificationType {
    const val GROUP_JOIN_REQUEST = "GROUP_JOIN_REQUEST"  // 방장에게: 누군가 그룹 참가 신청
    const val GROUP_JOIN_APPROVED = "GROUP_JOIN_APPROVED"  // 신청자에게: 참가 신청 승인됨
    const val GROUP_JOIN_REJECTED = "GROUP_JOIN_REJECTED"  // 신청자에게: 참가 신청 거절됨
    const val GROUP_PERMISSION_CHANGED = "GROUP_PERMISSION_CHANGED"  // 그룹원에게: 권한 변경
}
