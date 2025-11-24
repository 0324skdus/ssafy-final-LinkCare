package com.a307.linkcare.feature.watch.ui

import androidx.lifecycle.ViewModel
import com.a307.linkcare.feature.watch.domain.respository.CustomizeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CustomizeRepoHolderVM @Inject constructor(
    val repo: CustomizeRepository
) : ViewModel()