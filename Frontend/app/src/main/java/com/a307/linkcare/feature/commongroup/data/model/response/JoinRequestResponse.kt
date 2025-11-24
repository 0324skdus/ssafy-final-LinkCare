package com.a307.linkcare.feature.commongroup.data.model.response

import com.google.gson.annotations.SerializedName

data class JoinRequestResponse(
    @SerializedName("requestSeq")
    val requestSeq: Long,

    @SerializedName("userSeq")
    val userSeq: Long,

    @SerializedName("userName")
    val userName: String,

    @SerializedName("userAge")
    val userAge: Int,

    @SerializedName("userGender")
    val userGender: String,

    @SerializedName("status")
    val status: String, // PENDING, APPROVED, REJECTED

    @SerializedName("requestedAt")
    val requestedAt: String
)
