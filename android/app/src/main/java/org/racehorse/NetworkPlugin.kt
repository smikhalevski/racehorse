package org.racehorse

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.AlertEvent
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.ResponseEvent

class OnlineStatusChangedAlertEvent(val online: Boolean) : AlertEvent

class IsOnlineRequestEvent : RequestEvent()

class IsOnlineResponseEvent(val online: Boolean) : ResponseEvent()

/**
 * Monitors network status.
 *
 * @param networkRequest The type of monitored network.
 */
class NetworkPlugin(
    private val networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()
) : Plugin() {

    private val online get() = onlineStatuses.values.contains(true)

    private val onlineStatuses = HashMap<Network, Boolean>()

    private val connectivityManager get() = activity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            onlineStatuses[network] = true
            post(OnlineStatusChangedAlertEvent(true))
        }

        override fun onLost(network: Network) {
            onlineStatuses[network] = false
            post(OnlineStatusChangedAlertEvent(online))
        }
    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
    }

    override fun onStop() {
        super.onStop()
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }

    @Subscribe
    fun onIsOnlineRequestEvent(event: IsOnlineRequestEvent) {
        postResponse(event, IsOnlineResponseEvent(online))
    }
}
