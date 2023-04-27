package org.racehorse

import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.postToChain

class AreNotificationsEnabledEvent : RequestEvent() {
    class ResultEvent(val isEnabled: Boolean) : ResponseEvent()
}

class NotificationsPlugin(
    private val activity: ComponentActivity,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    private val notificationManager get() = NotificationManagerCompat.from(activity)

    @Subscribe
    fun onAreNotificationsEnabled(event: AreNotificationsEnabledEvent) {
        eventBus.postToChain(event, AreNotificationsEnabledEvent.ResultEvent(notificationManager.areNotificationsEnabled()))
    }
}
