package org.racehorse

import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.NetworkMonitor
import org.racehorse.webview.*

class IsOnlineRequestEvent : RequestEvent()

class IsOnlineResponseEvent(val online: Boolean) : ResponseEvent()

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
