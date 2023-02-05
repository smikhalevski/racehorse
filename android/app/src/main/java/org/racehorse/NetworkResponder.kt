package org.racehorse

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.AlertEvent
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.ResponseEvent
import org.racehorse.webview.respond

class OnlineStatusChangedAlertEvent(val online: Boolean) : AlertEvent

class IsOnlineRequestEvent : RequestEvent()

class IsOnlineResponseEvent(val online: Boolean) : ResponseEvent()

class NetworkStatusResponder(context: Context, private val eventBus: EventBus) {

    private val networkOnlineStatuses = HashMap<Network, Boolean>()

    private val online get() = networkOnlineStatuses.values.contains(true)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            networkOnlineStatuses[network] = true
            eventBus.post(OnlineStatusChangedAlertEvent(true))
        }

        override fun onLost(network: Network) {
            networkOnlineStatuses[network] = false
            eventBus.post(OnlineStatusChangedAlertEvent(online))
        }
    }

    init {
        // TODO Must be unregistered
        @Suppress("UNNECESSARY_SAFE_CALL")
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)?.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build(),
            networkCallback
        )
    }

    @Subscribe
    fun onIsOnlineRequestEvent(event: IsOnlineRequestEvent) {
        event.respond(IsOnlineResponseEvent(online))
    }
}
