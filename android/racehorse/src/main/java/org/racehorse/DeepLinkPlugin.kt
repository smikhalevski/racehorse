@file:UseSerializers(IntentSerializer::class)

package org.racehorse

import android.content.Intent
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import org.greenrobot.eventbus.Subscribe
import org.racehorse.serializers.IntentSerializer

/**
 * Get the latest deep link dispatched via [OpenDeepLinkEvent].
 */
@Serializable
class GetLastDeepLinkEvent : RequestEvent() {

    /**
     * @param intent The most recent intent that was dispatched via [OpenDeepLinkEvent].
     */
    @Serializable
    class ResultEvent(val intent: Intent?) : ResponseEvent()
}

/**
 * Post this event to notify web that the new deep link intent has arrived.
 */
@Serializable
class OpenDeepLinkEvent(val intent: Intent) : NoticeEvent

/**
 * Provides access to app deep links.
 *
 * You should post [OpenDeepLinkEvent] in [androidx.appcompat.app.AppCompatActivity.onCreate] and
 * [androidx.appcompat.app.AppCompatActivity.onNewIntent] to make this plugin work.
 */
open class DeepLinkPlugin {

    private var lastIntent: Intent? = null

    @Subscribe
    open fun onOpenDeepLink(event: OpenDeepLinkEvent) {
        lastIntent = event.intent
    }

    @Subscribe
    open fun onGetLastDeepLink(event: GetLastDeepLinkEvent) {
        event.respond(GetLastDeepLinkEvent.ResultEvent(lastIntent))
    }
}
