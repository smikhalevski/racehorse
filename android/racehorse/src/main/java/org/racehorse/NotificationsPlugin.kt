package org.racehorse

import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import org.greenrobot.eventbus.Subscribe

class AreNotificationsEnabledEvent : RequestEvent() {
    class ResultEvent(val isEnabled: Boolean) : ResponseEvent()
}

class NotificationsPlugin(private val activity: ComponentActivity) {

    private val notificationManager get() = NotificationManagerCompat.from(activity)

    @Subscribe
    fun onAreNotificationsEnabled(event: AreNotificationsEnabledEvent) {
        event.respond(AreNotificationsEnabledEvent.ResultEvent(notificationManager.areNotificationsEnabled()))
    }
}
