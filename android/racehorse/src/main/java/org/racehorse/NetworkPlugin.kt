package org.racehorse

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
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
open class NetworkPlugin(private val networkMonitor: NetworkMonitor) : Plugin(), EventBusCapability {

    @Subscribe
    fun onIsOnlineRequestEvent(event: IsOnlineRequestEvent) {
        postToChain(event, IsOnlineResponseEvent(networkMonitor.isOnline))
    }
}

/**
 * Watches default app networks and posts [OnlineStatusChangedAlertEvent] when online status is changed.
 */
open class NetworkMonitor(private val eventBus: EventBus, private val context: Context) {
    var isOnline = false
        private set

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            isOnline = true
            eventBus.post(OnlineStatusChangedAlertEvent(true))
        }

        override fun onLost(network: Network) {
            isOnline = false
            eventBus.post(OnlineStatusChangedAlertEvent(false))
        }
    }

    /**
     * Enables network monitoring and posts [OnlineStatusChangedAlertEvent] when network state is changed.
     */
    fun start() {
        with(connectivityManager) {
            val online =
                getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            isOnline = online
            eventBus.post(OnlineStatusChangedAlertEvent(online))

            registerDefaultNetworkCallback(networkCallback)
        }
    }

    /**
     * Stops network monitoring.
     */
    fun stop() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
