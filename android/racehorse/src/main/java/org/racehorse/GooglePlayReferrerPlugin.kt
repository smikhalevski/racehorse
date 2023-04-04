package org.racehorse

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.*

class GetGooglePlayReferrerRequestEvent : RequestEvent()

class GetGooglePlayReferrerResponseEvent(val referrer: String?) : ResponseEvent()

class GooglePlayReferrerDetectedAlertEvent(val referrer: String) : AlertEvent

/**
 * Gets [Google Play referrer](https://developer.android.com/google/play/installreferrer/library) information.
 */
open class GooglePlayReferrerPlugin : Plugin(), EventBusCapability {

    companion object {
        const val GOOGLE_PLAY_REFERRER_KEY = "googlePlayReferrer"
    }

    private val preferences
        get() = context.getSharedPreferences(GooglePlayReferrerPlugin::class.java.name, Context.MODE_PRIVATE)

    private var referrerClient: InstallReferrerClient? = null

    @Subscribe
    fun onGetGooglePlayReferrerRequestEvent(event: GetGooglePlayReferrerRequestEvent) {
        val referrer = preferences.getString(GOOGLE_PLAY_REFERRER_KEY, null)

        postToChain(event, GetGooglePlayReferrerResponseEvent(referrer))

        if (referrer != null || referrerClient != null) {
            return
        }

        referrerClient = InstallReferrerClient.newBuilder(context).build().apply {
            startConnection(object : InstallReferrerStateListener {

                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (responseCode != InstallReferrerClient.InstallReferrerResponse.OK) {
                        return
                    }

                    val installReferrer = installReferrer.installReferrer

                    preferences.edit().putString(GOOGLE_PLAY_REFERRER_KEY, installReferrer).apply()
                    post(GooglePlayReferrerDetectedAlertEvent(installReferrer))
                }

                override fun onInstallReferrerServiceDisconnected() {}
            })
        }
    }
}
