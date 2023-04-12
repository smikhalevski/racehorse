package org.racehorse

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.NoticeEvent
import org.racehorse.RequestEvent
import org.racehorse.ResponseEvent
import org.racehorse.utils.postToChain

/**
 * Triggered by the web to retrieve the latest deep link.
 */
class GetLastDeepLinkRequestEvent : RequestEvent()

class GetLastDeepLinkResponseEvent(val url: String?) : ResponseEvent()

/**
 * Post this event to notify web that the new deep link intent has arrived.
 */
class OpenDeepLinkEvent(val url: String) : NoticeEvent

/**
 * Provides access to app deep links.
 *
 * @param eventBus The event bus to which events are posted.
 */
open class DeepLinkPlugin(private val eventBus: EventBus = EventBus.getDefault()) {

    private var lastUrl: String? = null

    @Subscribe
    open fun onOpenDeepLink(event: OpenDeepLinkEvent) {
        lastUrl = event.url
    }

    @Subscribe
    open fun onGetLastDeepLink(event: GetLastDeepLinkRequestEvent) {
        eventBus.postToChain(event, GetLastDeepLinkResponseEvent(lastUrl))
    }
}
