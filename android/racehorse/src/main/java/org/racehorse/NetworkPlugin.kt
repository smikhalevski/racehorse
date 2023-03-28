package org.racehorse

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.*

class IsOnlineRequestEvent : RequestEvent()

class IsOnlineResponseEvent(val isOnline: Boolean) : ResponseEvent()

class OnlineStatusChangedAlertEvent(val isOnline: Boolean) : AlertEvent

/**
 * Monitors network status.
 *
 * @param networkMonitor The type of monitored network.
 */
class NetworkPlugin(private val networkMonitor: NetworkMonitor) : Plugin(), EventBusCapability {

    @Subscribe
    fun onIsOnlineRequestEvent(event: IsOnlineRequestEvent) {
        postToChain(event, IsOnlineResponseEvent(networkMonitor.isOnline))
    }
}

open class NetworkMonitor(
    private val eventBus: EventBus,
    private val context: Context,
    private val networkRequest: NetworkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()
) {
    val isOnline get() = activeNetworks.isNotEmpty()

    private val activeNetworks = HashSet<Network>()

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (isOnline != updateOnlineStatus(network, true)) {
                eventBus.post(OnlineStatusChangedAlertEvent(true))
            }
        }

        override fun onLost(network: Network) {
            if (isOnline != updateOnlineStatus(network, false)) {
                eventBus.post(OnlineStatusChangedAlertEvent(false))
            }
        }
    }

    private fun updateOnlineStatus(network: Network, online: Boolean): Boolean {
        if (online) {
            activeNetworks.add(network)
        } else {
            activeNetworks.remove(network)
        }
        return activeNetworks.isNotEmpty()
    }

    fun start() {
        connectivityManager?.run {
            registerNetworkCallback(networkRequest, networkCallback)

            activeNetwork?.let { activeNetworks.add(it) }

            eventBus.post(OnlineStatusChangedAlertEvent(isOnline))
        }
    }

    fun stop() {
        activeNetworks.clear()

        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }
}
