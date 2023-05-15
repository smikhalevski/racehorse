package org.racehorse.auth

import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.RequestEvent
import org.racehorse.ResponseEvent
import org.racehorse.utils.launchActivityForResult
import org.racehorse.utils.postToChain

/**
 * Check for existing Google Sign-In account, if the user is already signed in the account will be non-null.
 */
class GetLastGoogleSignedInAccountEvent : RequestEvent() {
    class ResultEvent(val account: GoogleSignInAccount?) : ResponseEvent()
}

class GoogleSignInEvent : RequestEvent() {
    class ResultEvent(val account: GoogleSignInAccount?) : ResponseEvent()
}

class GoogleSignOutEvent : RequestEvent() {
    class ResultEvent : ResponseEvent()
}

class GoogleRevokeAccessEvent : RequestEvent() {
    class ResultEvent : ResponseEvent()
}

/**
 * @param googleSignInOptions Options to initialize Google Sign-In. Bu default, sign-in is configured to request the
 * user's ID, email address, and basic profile. ID and basic profile are included in
 * [GoogleSignInOptions.DEFAULT_SIGN_IN].
 */
open class GoogleSignInPlugin(
    private val activity: ComponentActivity,
    private val googleSignInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build(),
    private val eventBus: EventBus = EventBus.getDefault()
) {

    private val googleSignInClient by lazy { GoogleSignIn.getClient(activity, googleSignInOptions) }

    @Subscribe
    open fun onGetLastGoogleSignedInAccount(event: GetLastGoogleSignedInAccountEvent) {
        eventBus.postToChain(
            event,
            GetLastGoogleSignedInAccountEvent.ResultEvent(GoogleSignIn.getLastSignedInAccount(activity))
        )
    }

    @Subscribe
    open fun onGoogleSignIn(event: GoogleSignInEvent) {
        val launched = activity.launchActivityForResult(googleSignInClient.signInIntent) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)

            val account = try {
                task.getResult(ApiException::class.java)
            } catch (_: ApiException) {
                null
            }
            eventBus.postToChain(event, GoogleSignInEvent.ResultEvent(account))
        }

        if (!launched) {
            eventBus.postToChain(event, GoogleSignInEvent.ResultEvent(null))
        }
    }

    @Subscribe
    open fun onGoogleSignOutEvent(event: GoogleSignOutEvent) {
        googleSignInClient.signOut().addOnCompleteListener(activity) {
            eventBus.postToChain(event, GoogleSignOutEvent.ResultEvent())
        }
    }

    @Subscribe
    open fun onGoogleRevokeAccess(event: GoogleRevokeAccessEvent) {
        googleSignInClient.revokeAccess().addOnCompleteListener(activity) {
            eventBus.postToChain(event, GoogleRevokeAccessEvent.ResultEvent())
        }
    }
}
