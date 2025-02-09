package org.racehorse

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe

/**
 * Returns the Firebase token associated with the device.
 */
@Serializable
class GetFirebaseTokenEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val token: String?) : ResponseEvent()
}

/**
 * Enables Firebase integration.
 */
open class FirebasePlugin {

    @Subscribe
    open fun onGetFirebaseToken(event: GetFirebaseTokenEvent) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            event.respond(GetFirebaseTokenEvent.ResultEvent(if (it.isSuccessful) it.result else null))
        }
    }
}
