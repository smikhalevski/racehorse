import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.webview.AlertEvent
import org.racehorse.webview.RequestEvent
import org.racehorse.webview.postToChain

class OnlineStatusChangedAlertEvent(val online: Boolean) : AlertEvent

class IsOnlineRequestEvent : RequestEvent()

class IsOnlineResponseEvent(val online: Boolean) : RequestEvent()

// https://developer.android.com/training/monitoring-device-state/connectivity-status-type
class NetworkStatusResponder(context: Context, private val eventBus: EventBus) {

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            eventBus.post(OnlineStatusChangedAlertEvent(true))
        }

        override fun onLost(network: Network) {
            eventBus.post(OnlineStatusChangedAlertEvent(false))
        }
    }

    private val online = false

    init {
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
        event.postToChain(IsOnlineResponseEvent(online))
    }
}
