package org.racehorse

import com.google.firebase.messaging.FirebaseMessaging
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.postToChain

/**
 * Returns the Firebase token associated with the device.
 */
class GetFirebaseTokenEvent : RequestEvent() {
    class ResultEvent(val token: String?) : ResponseEvent()
}

/**
 * Enables Firebase integration.
 *
 * @param eventBus The event bus to which events are posted.
 */
open class FirebasePlugin(private val eventBus: EventBus = EventBus.getDefault()) {

    @Subscribe
    open fun onGetFirebaseToken(event: GetFirebaseTokenEvent) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            eventBus.postToChain(event, GetFirebaseTokenEvent.ResultEvent(if (it.isSuccessful) it.result else null))
        }
    }
}
