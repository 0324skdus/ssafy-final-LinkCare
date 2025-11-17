package com.a307.linkcare.feature.watch.service

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WorkoutServiceEntryPoint {
    fun inject(service: WatchListenerService)
}
