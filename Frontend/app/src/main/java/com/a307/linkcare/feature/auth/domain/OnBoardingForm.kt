package com.a307.linkcare.feature.auth.domain

data class OnboardingForm(
    val gender: String = "",
    val birth: String = "",
    val height: Float? = null,
    val weight: Float? = null,
    val exerciseStartYear: Int? = null,
    val characterId: Long = 0L,
    val petName: String = ""
)