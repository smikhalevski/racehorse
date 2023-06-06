package org.racehorse.auth

import androidx.activity.ComponentActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.RequestEvent
import org.racehorse.ResponseEvent

class GetCurrentFacebookAccessTokenEvent : RequestEvent() {
    class ResultEvent(val accessToken: AccessToken?) : ResponseEvent()
}

class FacebookLogInEvent(val permissions: Array<String> = arrayOf()) : RequestEvent() {
    class ResultEvent(val accessToken: AccessToken?) : ResponseEvent()
}

class FacebookLogOutEvent : RequestEvent() {
    class ResultEvent : ResponseEvent()
}

open class FacebookLoginPlugin(
    private val activity: ComponentActivity,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    val loginManager by lazy { LoginManager.getInstance() }

    @Subscribe
    fun onGetCurrentFacebookAccessToken(event: GetCurrentFacebookAccessTokenEvent) {
        event.respond(GetCurrentFacebookAccessTokenEvent.ResultEvent(AccessToken.getCurrentAccessToken()))
    }

    @Subscribe
    fun onFacebookLogIn(event: FacebookLogInEvent) {
        val callbackManager = CallbackManager.Factory.create()

        loginManager.registerCallback(callbackManager, object : FacebookCallback<LoginResult> {

            override fun onSuccess(result: LoginResult) = handleLoginResult(result)

            override fun onCancel() = handleLoginResult(null)

            override fun onError(error: FacebookException) = handleLoginResult(null)

            fun handleLoginResult(result: LoginResult?) {
                loginManager.unregisterCallback(callbackManager)
                event.respond(FacebookLogInEvent.ResultEvent(result?.accessToken))
            }
        })

        loginManager.logInWithReadPermissions(activity, callbackManager, event.permissions.toList())
    }

    @Subscribe
    fun onFacebookLogOut(event: FacebookLogOutEvent) {
        loginManager.logOut()
        event.respond(FacebookLogOutEvent.ResultEvent())
    }
}
