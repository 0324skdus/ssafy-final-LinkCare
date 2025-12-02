package com.a307.linkcare.common.di.module

import com.a307.linkcare.feature.notification.manager.NotificationEventManager
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