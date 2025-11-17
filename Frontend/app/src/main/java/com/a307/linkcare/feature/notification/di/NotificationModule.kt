package com.a307.linkcare.feature.notification.di

import com.a307.linkcare.feature.notification.data.NotificationEventManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Provides
    @Singleton
    fun provideNotificationEventManager(): NotificationEventManager {
        return NotificationEventManager()
    }
}
