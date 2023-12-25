package org.racehorse

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.google.gson.annotations.SerializedName
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.Serializable

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

data class NetworkStatus(val type: NetworkType, val isConnected: Boolean) : Serializable

class NetworkStatusChangedEvent(val status: NetworkStatus) : NoticeEvent

class GetNetworkStatusEvent : RequestEvent() {
    class ResultEvent(val status: NetworkStatus) : ResponseEvent()
}

/**
 * Monitors network status, watches default app networks and posts [NetworkStatusChangedEvent] when online status is
 * changed.
 *
 * @param context The context that provides access to [ConnectivityManager].
 * @param eventBus The event bus to which events are posted.
 */
open class NetworkPlugin(private val context: Context, private val eventBus: EventBus = EventBus.getDefault()) {

    val status get() = lastStatus ?: getNetworkStatus(capabilities)

    private var lastStatus: NetworkStatus? = null

    private val connectivityManager by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager }

    private val capabilities get() = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {

        override fun onLost(network: Network) =
            handleChange(null)

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) =
            handleChange(capabilities)

        private fun handleChange(capabilities: NetworkCapabilities?) = synchronized(this) {
            val status = getNetworkStatus(capabilities)
            if (status == lastStatus) {
                return
            }
            lastStatus = status
            eventBus.post(NetworkStatusChangedEvent(status))
        }
    }

    open fun enable() {
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        lastStatus = getNetworkStatus(capabilities)
    }

    open fun disable() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        lastStatus = null
    }

    @Subscribe
    open fun onGetNetworkStatus(event: GetNetworkStatusEvent) {
        event.respond(GetNetworkStatusEvent.ResultEvent(status))
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
