package org.racehorse

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.google.gson.annotations.SerializedName
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.postToChain

class GetNetworkStatusRequestEvent : RequestEvent()

class GetNetworkStatusResponseEvent(val status: NetworkStatus) : ResponseEvent()

class NetworkStatusChangedEvent(val status: NetworkStatus) : NoticeEvent

data class NetworkStatus(val type: NetworkType, val isConnected: Boolean)

enum class NetworkType {
    @SerializedName("wifi")
    WIFI,

    @SerializedName("cellular")
    CELLULAR,

    @SerializedName("none")
    NONE,

    @SerializedName("unknown")
    UNKNOWN
}

/**
 * Monitors network status, watches default app networks and posts [NetworkStatusChangedEvent] when online status is
 * changed.
 *
 * @param context The context that provides access to [ConnectivityManager].
 * @param eventBus The event bus to which events are posted.
 */
open class NetworkPlugin(private val context: Context, private val eventBus: EventBus = EventBus.getDefault()) {

    var networkStatus = getNetworkStatus(capabilities)

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val capabilities get() = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onAvailable(network: Network) =
            handleChange(capabilities)

        override fun onLost(network: Network) =
            handleChange(capabilities)

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            handleChange(capabilities)

        private fun handleChange(capabilities: NetworkCapabilities?) = synchronized(this) {
            val status = getNetworkStatus(capabilities)
            if (status == networkStatus) {
                return
            }
            networkStatus = status
            eventBus.post(NetworkStatusChangedEvent(status))
        }
    }

    open fun enable() = connectivityManager.registerDefaultNetworkCallback(networkCallback)

    open fun disable() = connectivityManager.unregisterNetworkCallback(networkCallback)

    @Subscribe
    open fun onGetNetworkStatus(event: GetNetworkStatusRequestEvent) {
        eventBus.postToChain(event, GetNetworkStatusResponseEvent(networkStatus))
    }

    protected fun getNetworkStatus(capabilities: NetworkCapabilities?): NetworkStatus {
        if (capabilities == null) {
            return NetworkStatus(NetworkType.NONE, false)
        }

        return NetworkStatus(
            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.CELLULAR
                else -> NetworkType.UNKNOWN
            },
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        )
    }
}
