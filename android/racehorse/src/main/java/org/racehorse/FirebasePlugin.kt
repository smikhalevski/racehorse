package org.racehorse

import com.google.firebase.messaging.FirebaseMessaging
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.EventBusCapability
import org.racehorse.webview.Plugin
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.ResponseEvent

class NewFirebaseTokenEvent(val token: String)

class GetFirebaseTokenRequestEvent : RequestEvent()

class GetFirebaseTokenResponseEvent(val token: String?) : ResponseEvent()

open class FirebasePlugin : Plugin(), EventBusCapability {

    @Subscribe
    fun onGetFirebaseTokenRequestEvent(event: GetFirebaseTokenRequestEvent) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            postToChain(event, GetFirebaseTokenResponseEvent(if (task.isSuccessful) task.result else null))
        }
    }
}
