package org.racehorse

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.*

class IsOnlineRequestEvent : RequestEvent()

class IsOnlineResponseEvent(val online: Boolean) : ResponseEvent()

class OnlineStatusChangedAlertEvent(val online: Boolean) : AlertEvent

/**
 * Monitors network status.
 *
 * @param networkMonitor The type of monitored network.
 */
class NetworkPlugin(private val networkMonitor: NetworkMonitor) : Plugin(), EventBusCapability {

    @Subscribe
    fun onIsOnlineRequestEvent(event: IsOnlineRequestEvent) {
        postToChain(event, IsOnlineResponseEvent(networkMonitor.online))
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
    var online = false
        private set

    private val onlineMap = HashMap<Network, Boolean>()

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (online != setNetworkOnline(network, true)) {
                eventBus.post(OnlineStatusChangedAlertEvent(true))
            }
        }

        override fun onLost(network: Network) {
            if (online != setNetworkOnline(network, false)) {
                eventBus.post(OnlineStatusChangedAlertEvent(false))
            }
        }
    }

    private fun setNetworkOnline(network: Network, networkOnline: Boolean): Boolean {
        onlineMap[network] = networkOnline
        return onlineMap.values.contains(true).also { online = it }
    }

    fun start() {
        connectivityManager?.run {
            online = if (Build.VERSION.SDK_INT < 23) {
                activeNetworkInfo?.isConnected ?: false
            } else {
                getNetworkCapabilities(activeNetwork ?: return@run false) != null
            }
            registerNetworkCallback(networkRequest, networkCallback)
        }
    }

    fun stop() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }
}
