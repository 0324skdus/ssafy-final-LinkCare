package com.a307.linkcare.feature.caregroup.data.model.request

data class Tap(
    val id: Long,
    val name: String,
    val text: String,
    val focused: Boolean = false
)
