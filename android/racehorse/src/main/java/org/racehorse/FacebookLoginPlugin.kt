package org.racehorse

import androidx.activity.ComponentActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import org.greenrobot.eventbus.Subscribe
import java.io.Serializable

class SerializableFacebookAccessToken(
    val expires: Long,
    val permissions: List<String>,
    val declinedPermissions: List<String>,
    val expiredPermissions: List<String>,
    val token: String,
    val lastRefresh: Long,
    val applicationId: String,
    val userId: String,
    val dataAccessExpirationTime: Long,
    val graphDomain: String?,
    val isExpired: Boolean,
    val isDataAccessExpired: Boolean,
    val isInstagramToken: Boolean,
) : Serializable {
    constructor(accessToken: AccessToken) : this(
        expires = accessToken.expires.time,
        permissions = accessToken.permissions.filterNotNull(),
        declinedPermissions = accessToken.declinedPermissions.filterNotNull(),
        expiredPermissions = accessToken.expiredPermissions.filterNotNull(),
        token = accessToken.token,
        lastRefresh = accessToken.lastRefresh.time,
        applicationId = accessToken.applicationId,
        userId = accessToken.userId,
        dataAccessExpirationTime = accessToken.dataAccessExpirationTime.time,
        graphDomain = accessToken.graphDomain,
        isExpired = accessToken.isExpired,
        isDataAccessExpired = accessToken.isDataAccessExpired,
        isInstagramToken = accessToken.isInstagramToken,
    )
}

class GetCurrentFacebookAccessTokenEvent : RequestEvent() {
    class ResultEvent(val accessToken: SerializableFacebookAccessToken?) : ResponseEvent()
}

class FacebookLogInEvent(val permissions: Array<String> = arrayOf()) : RequestEvent() {
    class ResultEvent(val accessToken: SerializableFacebookAccessToken?) : ResponseEvent()
}

class FacebookLogOutEvent : RequestEvent()

open class FacebookLoginPlugin(private val activity: ComponentActivity) {

    val loginManager by lazy { LoginManager.getInstance() }

    @Subscribe
    fun onGetCurrentFacebookAccessToken(event: GetCurrentFacebookAccessTokenEvent) {
        event.respond(
            GetCurrentFacebookAccessTokenEvent.ResultEvent(
                AccessToken.getCurrentAccessToken()?.let(::SerializableFacebookAccessToken)
            )
        )
    }

    @Subscribe
    fun onFacebookLogIn(event: FacebookLogInEvent) {
        val callbackManager = CallbackManager.Factory.create()

        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {

            override fun onSuccess(result: LoginResult) = handleLoginResult(result)

            override fun onCancel() = handleLoginResult(null)

            override fun onError(error: FacebookException) = handleLoginResult(null)

            fun handleLoginResult(result: LoginResult?) = event.respond {
                loginManager.unregisterCallback(callbackManager)

                FacebookLogInEvent.ResultEvent(result?.accessToken?.let(::SerializableFacebookAccessToken))
            }
        })

        loginManager.logInWithReadPermissions(activity, callbackManager, event.permissions.toList())
    }

    @Subscribe
    fun onFacebookLogOut(event: FacebookLogOutEvent) {
        event.respond {
            loginManager.logOut()
            VoidEvent()
        }
    }
}
