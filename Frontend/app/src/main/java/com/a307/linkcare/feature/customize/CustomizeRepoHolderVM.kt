package com.a307.linkcare.feature.customize

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CustomizeRepoHolderVM @Inject constructor(
    val repo: CustomizeRepository
) : ViewModel()
