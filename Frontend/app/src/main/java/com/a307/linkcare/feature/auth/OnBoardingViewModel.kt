package com.a307.linkcare.feature.auth


import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.a307.linkcare.feature.auth.domain.OnboardingForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OnboardingViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object { private const val KEY = "onboarding_form" }

    private val _form = MutableStateFlow(
        savedStateHandle.get<OnboardingForm>(KEY) ?: OnboardingForm()
    )
    val form = _form.asStateFlow()

    private fun update(block: (OnboardingForm) -> OnboardingForm) {
        _form.update(block)
        savedStateHandle[KEY] = _form.value
    }

    fun setName(v: String) = update { it.copy(name = v) }
    fun setGender(v: String) = update { it.copy(gender = v) }
    fun setBirth(v: String) = update { it.copy(birth = v) }
    fun setHeight(v: Int?) = update { it.copy(height = v) }
    fun setCharacter(v: Int?) = update { it.copy(characterIndex = v) }

    fun isComplete() = with(_form.value) {
        name.isNotBlank() && gender.isNotBlank() && birth.isNotBlank()
                && height != null && characterIndex != null
    }
}