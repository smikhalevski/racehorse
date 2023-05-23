package org.racehorse

import android.content.Intent
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.WebIntent

/**
 * Triggered by the web to retrieve the latest deep link.
 */
class GetLastDeepLinkEvent : RequestEvent() {

    /**
     * @param intent The most recent intent that was dispatched via [OpenDeepLinkEvent].
     */
    class ResultEvent(val intent: WebIntent?) : ResponseEvent()
}

/**
 * Post this event to notify web that the new deep link intent has arrived.
 */
class OpenDeepLinkEvent(val intent: WebIntent) : NoticeEvent {

    constructor(intent: Intent) : this(WebIntent(intent))
}

/**
 * Provides access to app deep links.
 *
 * You should post [OpenDeepLinkEvent] in [androidx.appcompat.app.AppCompatActivity.onCreate] and
 * [androidx.appcompat.app.AppCompatActivity.onNewIntent] to make this plugin work.
 */
open class DeepLinkPlugin {

    private var lastIntent: WebIntent? = null

    @Subscribe
    open fun onOpenDeepLink(event: OpenDeepLinkEvent) {
        lastIntent = event.intent
    }

    @Subscribe
    open fun onGetLastDeepLink(event: GetLastDeepLinkEvent) {
        event.respond(GetLastDeepLinkEvent.ResultEvent(lastIntent))
    }
}
