package com.a307.linkcare.feature.commongroup.data.model.response

import com.google.gson.annotations.SerializedName

data class InvitationResponse(
    @SerializedName("invitationSeq")
    val invitationSeq: Long,

    @SerializedName("groupSeq")
    val groupSeq: Long,

    @SerializedName("groupName")
    val groupName: String,

    @SerializedName("invitationToken")
    val invitationToken: String,

    @SerializedName("createdAt")
    val createdAt: String,

    @SerializedName("expiredAt")
    val expiredAt: String,

    @SerializedName("invitationUrl")
    val invitationUrl: String?
)
