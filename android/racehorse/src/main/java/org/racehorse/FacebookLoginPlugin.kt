package org.racehorse

import androidx.activity.ComponentActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.Subscribe

@Serializable
class AccessTokenSurrogate(
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
) {
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

@Serializable
class GetCurrentFacebookAccessTokenEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val accessToken: AccessTokenSurrogate?) : ResponseEvent()
}

@Serializable
class FacebookLogInEvent(val permissions: List<String> = emptyList()) : RequestEvent() {

    @Serializable
    class ResultEvent(val accessToken: AccessTokenSurrogate?) : ResponseEvent()
}

@Serializable
class FacebookLogOutEvent : RequestEvent()

open class FacebookLoginPlugin(private val activity: ComponentActivity) {

    val loginManager by lazy { LoginManager.getInstance() }

    @Subscribe
    fun onGetCurrentFacebookAccessToken(event: GetCurrentFacebookAccessTokenEvent) {
        event.respond(
            GetCurrentFacebookAccessTokenEvent.ResultEvent(
                AccessToken.getCurrentAccessToken()?.let(::AccessTokenSurrogate)
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

                FacebookLogInEvent.ResultEvent(result?.accessToken?.let(::AccessTokenSurrogate))
            }
        })

        loginManager.logInWithReadPermissions(activity, callbackManager, event.permissions)
    }

    @Subscribe
    fun onFacebookLogOut(event: FacebookLogOutEvent) {
        event.respond {
            loginManager.logOut()
            VoidEvent
        }
    }
}
