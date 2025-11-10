package com.a307.linkcare.feature.auth.domain

data class OnboardingForm(
    val name: String = "",
    val gender: String = "",
    val birth: String = "",
    val height: Int? = null,
    val characterIndex: Int? = null
)