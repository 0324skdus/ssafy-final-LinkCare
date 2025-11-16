package com.a307.linkcare.feature.notification.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationEventManager @Inject constructor() {

    private val _newNotificationEvent = MutableSharedFlow<Unit>(replay = 0)
    val newNotificationEvent: SharedFlow<Unit> = _newNotificationEvent.asSharedFlow()

    suspend fun notifyNewNotification() {
        _newNotificationEvent.emit(Unit)
    }
}
