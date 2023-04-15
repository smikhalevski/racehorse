package org.racehorse

import com.google.firebase.messaging.FirebaseMessaging
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.postToChain

class GetFirebaseTokenRequestEvent : RequestEvent()

class GetFirebaseTokenResponseEvent(val token: String?) : ResponseEvent()

/**
 * Enables Firebase integration.
 *
 * @param eventBus The event bus to which events are posted.
 */
open class FirebasePlugin(private val eventBus: EventBus = EventBus.getDefault()) {

    @Subscribe
    open fun onGetFirebaseToken(event: GetFirebaseTokenRequestEvent) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            eventBus.postToChain(event, GetFirebaseTokenResponseEvent(if (it.isSuccessful) it.result else null))
        }
    }
}
