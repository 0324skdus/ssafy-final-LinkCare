package com.a307.linkcare.feature.commongroup.data.model.response

import com.google.gson.annotations.SerializedName

data class MyGroupResponse(
    @SerializedName("groupSeq") val groupSeq: Long,
    @SerializedName("groupName") val groupName: String,
    @SerializedName("groupDescription") val groupDescription: String,
    @SerializedName("type") val type: String,       // HEALTH / CARE
    @SerializedName("capacity") val capacity: Int,
    @SerializedName("currentMembers") val currentMembers: Int,
    @SerializedName("imageUrl") val imageUrl: String,
    @SerializedName("createdAt") val createdAt: String,
    @SerializedName("joinStatus") val joinStatus: String = "NONE"  // "NONE", "PENDING", "MEMBER"
)