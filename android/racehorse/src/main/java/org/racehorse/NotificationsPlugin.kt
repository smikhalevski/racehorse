package org.racehorse

import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe

@Serializable
class AreNotificationsEnabledEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val isEnabled: Boolean) : ResponseEvent()
}

class NotificationsPlugin(private val activity: ComponentActivity) {

    private val notificationManager get() = NotificationManagerCompat.from(activity)

    @Subscribe
    fun onAreNotificationsEnabled(event: AreNotificationsEnabledEvent) {
        event.respond(AreNotificationsEnabledEvent.ResultEvent(notificationManager.areNotificationsEnabled()))
    }
}
