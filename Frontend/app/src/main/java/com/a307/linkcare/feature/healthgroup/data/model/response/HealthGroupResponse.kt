package com.a307.linkcare.feature.healthgroup.data.model.response

data class HealthGroupResponse(
    val groupSeq: Int,
    val groupName: String,
    val groupDescription: String,
    val type: String,
    val capacity: Int,
    val currentMembers: Int,
    val imageUrl: String,
    val createdAt: String
)