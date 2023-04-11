package org.racehorse

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.postToChain

class IsOnlineRequestEvent : RequestEvent()

class IsOnlineResponseEvent(val isOnline: Boolean) : ResponseEvent()

class OnlineStatusChangedEvent(val isOnline: Boolean) : OutboundEvent

/**
 * Monitors network status, watches default app networks and posts [OnlineStatusChangedEvent] when online status is
 * changed.
 */
open class NetworkController(private val context: Context, private val eventBus: EventBus = EventBus.getDefault()) {

    var isOnline = false
        private set

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) {
            isOnline = true
            eventBus.post(OnlineStatusChangedEvent(true))
        }

        override fun onLost(network: Network) {
            isOnline = false
            eventBus.post(OnlineStatusChangedEvent(false))
        }
    }

    /**
     * Enables network monitoring and posts [OnlineStatusChangedEvent] when network state is changed.
     */
    fun start() {
        connectivityManager.apply {
            val online =
                getNetworkCapabilities(activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

            isOnline = online
            eventBus.post(OnlineStatusChangedEvent(online))

            registerDefaultNetworkCallback(networkCallback)
        }
    }

    /**
     * Stops network monitoring.
     */
    fun stop() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }

    @Subscribe
    fun onIsOnline(event: IsOnlineRequestEvent) {
        eventBus.postToChain(event, IsOnlineResponseEvent(isOnline))
    }
}
