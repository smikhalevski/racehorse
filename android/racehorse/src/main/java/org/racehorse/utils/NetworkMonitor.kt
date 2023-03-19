package org.racehorse.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import org.greenrobot.eventbus.EventBus
import org.racehorse.webview.AlertEvent

class OnlineStatusChangedAlertEvent(val online: Boolean) : AlertEvent

class NetworkMonitor(
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
        online = connectivityManager?.activeNetworkInfo?.isConnected ?: false
        connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun stop() {
        connectivityManager?.unregisterNetworkCallback(networkCallback)
    }
}
