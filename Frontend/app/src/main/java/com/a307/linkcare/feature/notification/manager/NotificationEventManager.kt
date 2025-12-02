package com.a307.linkcare.feature.notification.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class NotificationEventManager {

    private val _newNotificationEvent = MutableSharedFlow<Unit>(replay = 1)
    val newNotificationEvent: SharedFlow<Unit> = _newNotificationEvent.asSharedFlow()

    suspend fun notifyNewNotification() {
        _newNotificationEvent.emit(Unit)
    }
}