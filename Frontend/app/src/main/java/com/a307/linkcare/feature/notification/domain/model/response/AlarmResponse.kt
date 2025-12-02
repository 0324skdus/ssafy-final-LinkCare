package com.a307.linkcare.feature.notification.domain.model.response

data class AlarmResponse(
    val alarmId: Long,
    val senderInfo: SenderInfo,
    val groupInfo: GroupInfo,
    val messageType: String,  // "POKE" or "LETTER"
    val content: String,
    val sentAt: String,  // ISO 8601 format: "2025-11-18T21:42:43.123Z"
    val read: Boolean
)

data class SenderInfo(
    val userPk: Long,
    val nickname: String
)

data class GroupInfo(
    val groupSeq: Long,
    val groupName: String,
    val groupType: String  // "HEALTH" or "CARE"
)
