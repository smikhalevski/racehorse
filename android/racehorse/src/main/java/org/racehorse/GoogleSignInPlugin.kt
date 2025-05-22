package org.racehorse

import androidx.activity.ComponentActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.apiResult
import org.racehorse.utils.launchActivityForResult

@Serializable
class GoogleSignInAccountSurrogate(
    val id: String?,
    val idToken: String?,
    val email: String?,
    val grantedScopes: List<String>,
    val serverAuthCode: String?,
    val displayName: String?,
    val givenName: String?,
    val familyName: String?,
    val photoUrl: String?,
    val isExpired: Boolean,
) {
    constructor(account: GoogleSignInAccount) : this(
        id = account.id,
        idToken = account.idToken,
        email = account.email,
        grantedScopes = account.grantedScopes.map { it.scopeUri },
        serverAuthCode = account.serverAuthCode,
        displayName = account.displayName,
        givenName = account.givenName,
        familyName = account.familyName,
        photoUrl = account.photoUrl.toString(),
        isExpired = account.isExpired,
    )
}

/**
 * Check for existing Google Sign-In account, if the user is already signed in the account will be non-null.
 */
@Serializable
class GetLastGoogleSignedInAccountEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val account: GoogleSignInAccountSurrogate?) : ResponseEvent()
}

@Serializable
class GoogleSignInEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val account: GoogleSignInAccountSurrogate?) : ResponseEvent()
}

@Serializable
class GoogleSilentSignInEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val account: GoogleSignInAccountSurrogate?) : ResponseEvent()
}

@Serializable
class GoogleSignOutEvent : RequestEvent()

@Serializable
class GoogleRevokeAccessEvent : RequestEvent()

/**
 * @param googleSignInOptions Options to initialize Google Sign-In. Bu default, sign-in is configured to request the
 * user's ID, email address, and basic profile. ID and basic profile are included in
 * [GoogleSignInOptions.DEFAULT_SIGN_IN].
 */
open class GoogleSignInPlugin(
    private val activity: ComponentActivity,
    private val googleSignInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build(),
) {

    private val googleSignInClient by lazy { GoogleSignIn.getClient(activity, googleSignInOptions) }

    @Subscribe
    open fun onGetLastGoogleSignedInAccount(event: GetLastGoogleSignedInAccountEvent) {
        val account = GoogleSignIn.getLastSignedInAccount(activity)?.let(::GoogleSignInAccountSurrogate)

        event.respond(GetLastGoogleSignedInAccountEvent.ResultEvent(account))
    }

    @Subscribe
    open fun onGoogleSignIn(event: GoogleSignInEvent) {
        val isLaunched = activity.launchActivityForResult(googleSignInClient.signInIntent) {
            event.respond {
                GoogleSignInEvent.ResultEvent(
                    try {
                        GoogleSignInAccountSurrogate(GoogleSignIn.getSignedInAccountFromIntent(it.data).apiResult)
                    } catch (e: ApiException) {
                        if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) null else throw e
                    }
                )
            }
        }

        if (!isLaunched) {
            event.respond(GoogleSignInEvent.ResultEvent(null))
        }
    }

    @Subscribe
    open fun onGoogleSilentSignIn(event: GoogleSilentSignInEvent) {
        val task = googleSignInClient.silentSignIn()

        if (task.isSuccessful) {
            event.respond(GoogleSilentSignInEvent.ResultEvent(GoogleSignInAccountSurrogate(task.apiResult)))
            return
        }

        task.addOnCompleteListener {
            event.respond {
                GoogleSilentSignInEvent.ResultEvent(
                    try {
                        GoogleSignInAccountSurrogate(task.apiResult)
                    } catch (e: ApiException) {
                        if (e.statusCode == GoogleSignInStatusCodes.SIGN_IN_REQUIRED) null else throw e
                    }
                )
            }
        }
    }

    @Subscribe
    open fun onGoogleSignOutEvent(event: GoogleSignOutEvent) {
        googleSignInClient.signOut().addOnCompleteListener(activity) {
            event.respond(VoidEvent())
        }
    }

    @Subscribe
    open fun onGoogleRevokeAccess(event: GoogleRevokeAccessEvent) {
        googleSignInClient.revokeAccess().addOnCompleteListener(activity) {
            event.respond(VoidEvent())
        }
    }
}
