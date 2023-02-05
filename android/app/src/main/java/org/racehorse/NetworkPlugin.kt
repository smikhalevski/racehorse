package org.racehorse

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.activity.ComponentActivity
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.AlertEvent
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.ResponseEvent
import org.racehorse.webview.respond

class OnlineStatusChangedAlertEvent(val online: Boolean) : AlertEvent

class IsOnlineRequestEvent : RequestEvent()

class IsOnlineResponseEvent(val online: Boolean) : ResponseEvent()

class NetworkPlugin : Plugin() {

    val online get() = onlineStatuses.values.contains(true)

    private val onlineStatuses = HashMap<Network, Boolean>()
    private var connectivityManager: ConnectivityManager? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onlineStatuses[network] = true
            eventBus.post(OnlineStatusChangedAlertEvent(true))
        }

        override fun onLost(network: Network) {
            onlineStatuses[network] = false
            eventBus.post(OnlineStatusChangedAlertEvent(online))
        }
    }

    override fun start() {
        eventBus.register(this)

        connectivityManager = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        connectivityManager?.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build(),
            networkCallback
        )
    }

    override fun stop() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
        eventBus.unregister(this)
    }

    @Subscribe
    fun onIsOnlineRequestEvent(event: IsOnlineRequestEvent) {
        event.respond(IsOnlineResponseEvent(online))
    }
}
