package org.racehorse

import android.net.Uri
import androidx.activity.ComponentActivity
import com.facebook.CallbackManager
import com.facebook.share.model.ShareHashtag
import com.facebook.share.model.ShareLinkContent
import com.facebook.share.widget.ShareDialog
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.launchActivityForResult

@Serializable
class FacebookShareLinkEvent(
    val quote: String? = null,
    val contentUrl: String? = null,
    val peopleIds: List<String>? = null,
    val placeId: String? = null,
    val pageId: String? = null,
    val ref: String? = null,
    val hashtag: String? = null,
) : RequestEvent()

class FacebookSharePlugin(val activity: ComponentActivity) {

    @Subscribe
    fun onFacebookShareLink(event: FacebookShareLinkEvent) {
        val callbackManager = CallbackManager.Factory.create()

        activity.launchActivityForResult(
            ShareDialog(activity).createActivityResultContractForShowingDialog(callbackManager),

            ShareLinkContent.Builder()
                .setQuote(event.quote)
                .setContentUrl(event.contentUrl?.let(Uri::parse))
                .setPeopleIds(event.peopleIds)
                .setPlaceId(event.placeId)
                .setPageId(event.pageId)
                .setRef(event.ref)
                .setShareHashtag(event.hashtag?.let { ShareHashtag.Builder().setHashtag(it).build() })
                .build()
        ) {
            event.respond(VoidEvent)
        }
    }
}
