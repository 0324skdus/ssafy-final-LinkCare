package com.a307.linkcare.feature.auth.ui.onboarding

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.auth.domain.repository.ProfileRepository
import com.a307.linkcare.feature.auth.data.model.request.OnboardingForm
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repo: ProfileRepository
) : ViewModel() {
    private val _form = MutableStateFlow(OnboardingForm())
    val form = _form.asStateFlow()

    init { Log.wtf("OnboardingVM", "CREATED: $this") }

    fun update(block: (OnboardingForm) -> OnboardingForm) {
        val updated = block(_form.value)
        _form.value = updated
    }

    fun setGender(v: String) = update { it.copy(gender = v) }
    fun setBirth(v: String) = update { it.copy(birth = v) }
    fun setHeight(v: Float?) = update { it.copy(height = v) }
    fun setWeight(v: Float?) = update { it.copy(weight = v) }
    fun setExerciseStartYear(v: Int?) = update { it.copy(exerciseStartYear = v) }
    fun setCharacterId(v: Long) = update { it.copy(characterId = v) }
    fun setCharacterName(v: String) = update { it.copy(petName = v) }

    fun isComplete() = with(_form.value) {
        gender.isNotBlank() && birth.isNotBlank() &&
                height != null && weight != null
        // exerciseStartYear는 제외
    }

    sealed interface SubmitState {
        data object Idle: SubmitState
        data object Loading: SubmitState
        data object Success: SubmitState
        data class Error(val message: String): SubmitState
    }
    private val _submit = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submit = _submit.asStateFlow()

    fun submitProfile() {
        val f = _form.value
        if (!isComplete()) {
            _submit.value = SubmitState.Error("필수 항목이 비어 있어요.")
            return
        }
        viewModelScope.launch {
            _submit.value = SubmitState.Loading
            _submit.value = try {
                repo.updateProfile(
                    birth = f.birth,
                    height = f.height,
                    weight = f.weight,
                    gender = f.gender,
                    exerciseStartYear = null,
                    petName = null
                )

                // 초기 캐릭터 선택 API
                repo.selectInitial(
                    characterId = f.characterId,
                    petName = f.petName
                )
                SubmitState.Success
            } catch (t: Throwable) {
                SubmitState.Error(t.message ?: "요청 실패")
            }
        }
    }
}
