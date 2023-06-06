package org.racehorse.auth

import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.ExceptionEvent
import org.racehorse.RequestEvent
import org.racehorse.ResponseEvent
import org.racehorse.utils.launchActivityForResult
import java.io.Serializable

class WebGoogleSignInAccount(
    val id: String?,
    val idToken: String?,
    val email: String?,
    val grantedScopes: List<String>,
    val serverAuthCode: String?,
    val isExpired: Boolean,
    val displayName: String?,
    val givenName: String?,
    val familyName: String?,
    val photoUrl: String?,
) : Serializable {
    constructor(account: GoogleSignInAccount) : this(
        id = account.id,
        idToken = account.idToken,
        email = account.email,
        grantedScopes = account.grantedScopes.map { it.scopeUri },
        serverAuthCode = account.serverAuthCode,
        isExpired = account.isExpired,
        displayName = account.displayName,
        givenName = account.givenName,
        familyName = account.familyName,
        photoUrl = account.photoUrl.toString(),
    )
}

/**
 * Check for existing Google Sign-In account, if the user is already signed in the account will be non-null.
 */
class GetLastGoogleSignedInAccountEvent : RequestEvent() {
    class ResultEvent(val account: WebGoogleSignInAccount?) : ResponseEvent()
}

class GoogleSignInEvent : RequestEvent() {
    class ResultEvent(val account: WebGoogleSignInAccount?) : ResponseEvent()
}

class GoogleSilentSignInEvent : RequestEvent() {
    class ResultEvent(val account: WebGoogleSignInAccount?) : ResponseEvent()
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
        val account = GoogleSignIn.getLastSignedInAccount(activity)?.let(::WebGoogleSignInAccount)

        event.respond(GetLastGoogleSignedInAccountEvent.ResultEvent(account))
    }

    @Subscribe
    open fun onGoogleSignIn(event: GoogleSignInEvent) {
        val launched = activity.launchActivityForResult(googleSignInClient.signInIntent) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(it.data)

            event.respond(
                try {
                    GoogleSignInEvent.ResultEvent(WebGoogleSignInAccount(task.getResult(ApiException::class.java)))
                } catch (e: ApiException) {
                    if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                        GoogleSignInEvent.ResultEvent(null)
                    } else {
                        ExceptionEvent(e)
                    }
                }
            )
        }

        if (!launched) {
            event.respond(GoogleSignInEvent.ResultEvent(null))
        }
    }

    @Subscribe
    open fun onGoogleSilentSignIn(event: GoogleSilentSignInEvent) {
        val task = googleSignInClient.silentSignIn()

        if (task.isSuccessful) {
            event.respond(GoogleSilentSignInEvent.ResultEvent(WebGoogleSignInAccount(task.result)))
            return
        }

        task.addOnCompleteListener {
            event.respond(
                try {
                    GoogleSignInEvent.ResultEvent(WebGoogleSignInAccount(task.getResult(ApiException::class.java)))
                } catch (e: ApiException) {
                    if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_REQUIRED) {
                        GoogleSignInEvent.ResultEvent(null)
                    } else {
                        ExceptionEvent(e)
                    }
                }
            )
        }
    }

    @Subscribe
    open fun onGoogleSignOutEvent(event: GoogleSignOutEvent) {
        googleSignInClient.signOut().addOnCompleteListener(activity) {
            event.respond(GoogleSignOutEvent.ResultEvent())
        }
    }

    @Subscribe
    open fun onGoogleRevokeAccess(event: GoogleRevokeAccessEvent) {
        googleSignInClient.revokeAccess().addOnCompleteListener(activity) {
            event.respond(GoogleRevokeAccessEvent.ResultEvent())
        }
    }
}
