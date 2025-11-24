package com.a307.linkcare.feature.healthgroup.data.model.response

data class ApiResponse<T>(
    val message: String,
    val status: Int,
    val data: T?
)
