package com.a307.linkcare.feature.commongroup.data.model.response

data class GroupResponse (
    val groupSeq: Int,
    val groupName: String,
    val groupDescription: String,
    val type: String,
    val capacity: Int,
    val currentMembers: Int,
    val imageUrl: String,
    val createdAt: String,
    val joinStatus: String
)
