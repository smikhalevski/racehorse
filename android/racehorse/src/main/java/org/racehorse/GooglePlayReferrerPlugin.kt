package org.racehorse

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.AlertEvent
import org.racehorse.webview.EventBusCapability
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.ResponseEvent

class GetGooglePlayReferrerRequestEvent : RequestEvent()

class GetGooglePlayReferrerResponseEvent(val referrer: String?) : ResponseEvent()

class GooglePlayReferrerDetectedAlertEvent(val referrer: String) : AlertEvent

/**
 * Resolves [Google Play referrer](https://developer.android.com/google/play/installreferrer/library).
 */
class GooglePlayReferrerPlugin : Plugin(), EventBusCapability {

    companion object {
        const val REFERRER_KEY = "referrer"
    }

    private val preferences
        get() = context.getSharedPreferences(GooglePlayReferrerPlugin::class.java.name, Context.MODE_PRIVATE)

    override fun onStart() {
        super.onStart()

        val referrerClient = InstallReferrerClient.newBuilder(context).build()

        referrerClient.startConnection(object : InstallReferrerStateListener {

            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                    val referrer = referrerClient.installReferrer.installReferrer

                    preferences.edit().putString(REFERRER_KEY, referrer).apply()
                    post(GooglePlayReferrerDetectedAlertEvent(referrer))
                }
            }

            override fun onInstallReferrerServiceDisconnected() {}
        })
    }

    @Subscribe
    fun onGetGooglePlayReferrerRequestEvent(event: GetGooglePlayReferrerRequestEvent) {
        postToChain(event, GetGooglePlayReferrerResponseEvent(preferences.getString(REFERRER_KEY, null)))
    }
}
