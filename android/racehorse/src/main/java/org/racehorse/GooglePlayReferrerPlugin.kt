package org.racehorse

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe

@Serializable
class GetGooglePlayReferrerEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val referrer: String?, val responseCode: Int) : ResponseEvent()
}

/**
 * Gets [Google Play referrer](https://developer.android.com/google/play/installreferrer/library) information.
 */
open class GooglePlayReferrerPlugin(private val context: Context) {

    @Subscribe
    open fun onGetGooglePlayReferrer(event: GetGooglePlayReferrerEvent) {
        val referrerClient = InstallReferrerClient.newBuilder(context).build()

        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) = handleResponse(responseCode)

            override fun onInstallReferrerServiceDisconnected() =
                handleResponse(InstallReferrerClient.InstallReferrerResponse.SERVICE_DISCONNECTED)

            private fun handleResponse(responseCode: Int) {
                val referrer = try {
                    referrerClient.installReferrer.installReferrer
                } catch (_: Throwable) {
                    null
                }

                event.respond(GetGooglePlayReferrerEvent.ResultEvent(referrer, responseCode))
            }
        })
    }
}
