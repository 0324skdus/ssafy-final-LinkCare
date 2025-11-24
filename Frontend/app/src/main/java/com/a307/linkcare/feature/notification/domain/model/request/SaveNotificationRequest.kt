package com.a307.linkcare.feature.notification.domain.model.request

/**
 * 알림 저장 요청 DTO
 * POST /api/alarms/save
 */
data class SaveNotificationRequest(
    val receiverUserPk: Long,
    val groupSeq: Long,
    val messageType: String,  // "POKE"
    val content: String
)