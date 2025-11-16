package com.a307.linkcare.feature.caregroup.domain

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a307.linkcare.feature.caregroup.HealthToday
import com.a307.linkcare.feature.caregroup.data.CareGroupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CareGroupHomeViewModel @Inject constructor(
    private val repo: CareGroupRepository
) : ViewModel() {

    private val _healthData = MutableStateFlow<Map<Int, HealthToday>>(emptyMap())
    val healthData: StateFlow<Map<Int, HealthToday>> = _healthData

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadHealthData(userSeq: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("CareGroupHomeVM", "Loading health data for userSeq=$userSeq")
                val data = repo.getDailyHealthDetail(userSeq)
                if (data != null) {
                    Log.d("CareGroupHomeVM", "Received health data for userSeq=$userSeq")
                    _healthData.value = _healthData.value + (userSeq to data)
                } else {
                    Log.d("CareGroupHomeVM", "No health data for userSeq=$userSeq")
                }
            } catch (e: Exception) {
                Log.e("CareGroupHomeVM", "Error loading health data for userSeq=$userSeq", e)
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadHealthDataForMembers(userSeqs: List<Int>) {
        Log.d("CareGroupHomeVM", "loadHealthDataForMembers called with ${userSeqs.size} members: $userSeqs")
        userSeqs.forEach { userSeq ->
            loadHealthData(userSeq)
        }
    }
}
