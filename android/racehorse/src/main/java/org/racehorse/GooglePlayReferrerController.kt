package org.racehorse

import android.content.Context
import android.content.SharedPreferences
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.postToChain

class GetGooglePlayReferrerRequestEvent : RequestEvent()

class GetGooglePlayReferrerResponseEvent(val referrer: String?) : ResponseEvent()

class GooglePlayReferrerDetectedEvent(val referrer: String) : NoticeEvent

const val GOOGLE_PLAY_REFERRER_KEY = "googlePlayReferrer"

/**
 * Gets [Google Play referrer](https://developer.android.com/google/play/installreferrer/library) information.
 */
open class GooglePlayReferrerController(
    private val context: Context,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("GooglePlayReferrerController", Context.MODE_PRIVATE)
    }

    private val referrerClient: InstallReferrerClient by lazy {
        InstallReferrerClient.newBuilder(context).build().apply {
            startConnection(object : InstallReferrerStateListener {

                override fun onInstallReferrerSetupFinished(responseCode: Int) {
                    if (responseCode == InstallReferrerClient.InstallReferrerResponse.OK) {
                        preferences.edit().putString(GOOGLE_PLAY_REFERRER_KEY, installReferrer.installReferrer).apply()
                        eventBus.post(GooglePlayReferrerDetectedEvent(installReferrer.installReferrer))
                    }
                }

                override fun onInstallReferrerServiceDisconnected() {}
            })
        }
    }

    @Subscribe
    open fun onGetGooglePlayReferrerRequestEvent(event: GetGooglePlayReferrerRequestEvent) {
        eventBus.postToChain(
            event,
            GetGooglePlayReferrerResponseEvent(preferences.getString(GOOGLE_PLAY_REFERRER_KEY, null))
        )
        referrerClient.isReady
    }
}
