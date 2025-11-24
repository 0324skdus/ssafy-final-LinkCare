package com.a307.linkcare.feature.caregroup.data.model.response

data class CareGroupResponse(
    val groupSeq: Int,
    val groupName: String,
    val groupDescription: String,
    val type: String,
    val capacity: Int,
    val currentMembers: Int,
    val imageUrl: String,
    val createdAt: String
)