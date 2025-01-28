@file:UseSerializers(AccessTokenSerializer::class)

package org.racehorse

import androidx.activity.ComponentActivity
import com.facebook.AccessToken
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure
import org.greenrobot.eventbus.Subscribe

@Serializable
class GetCurrentFacebookAccessTokenEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val accessToken: AccessToken?) : ResponseEvent()
}

@Serializable
class FacebookLogInEvent(val permissions: List<String> = emptyList()) : RequestEvent() {

    @Serializable
    class ResultEvent(val accessToken: AccessToken?) : ResponseEvent()
}

@Serializable
class FacebookLogOutEvent : RequestEvent()

open class FacebookLoginPlugin(private val activity: ComponentActivity) {

    val loginManager by lazy { LoginManager.getInstance() }

    @Subscribe
    fun onGetCurrentFacebookAccessToken(event: GetCurrentFacebookAccessTokenEvent) {
        event.respond(
            GetCurrentFacebookAccessTokenEvent.ResultEvent(AccessToken.getCurrentAccessToken())
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

                FacebookLogInEvent.ResultEvent(result?.accessToken)
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

object AccessTokenSerializer : KSerializer<AccessToken> {
    override val descriptor = buildClassSerialDescriptor(AccessToken::class.java.simpleName) {
        element<Long>("expires")
        element<List<String>>("permissions")
        element<List<String>>("declinedPermissions")
        element<List<String>>("expiredPermissions")
        element<String>("token")
        element<Long>("lastRefresh")
        element<String>("applicationId")
        element<String>("userId")
        element<Long>("dataAccessExpirationTime")
        element<String?>("graphDomain")
        element<Boolean>("isExpired")
        element<Boolean>("isDataAccessExpired")
        element<Boolean>("isInstagramToken")
    }

    @ExperimentalSerializationApi
    override fun serialize(encoder: Encoder, value: AccessToken) {
        encoder.encodeStructure(descriptor) {
            encodeLongElement(descriptor, 0, value.expires.time)
            encodeSerializableElement(descriptor, 1, ListSerializer(String.serializer()), value.permissions.filterNotNull())
            encodeSerializableElement(descriptor, 2, ListSerializer(String.serializer()), value.declinedPermissions.filterNotNull())
            encodeSerializableElement(descriptor, 3, ListSerializer(String.serializer()), value.expiredPermissions.filterNotNull())
            encodeStringElement(descriptor, 4, value.token)
            encodeLongElement(descriptor, 5, value.lastRefresh.time)
            encodeStringElement(descriptor, 6, value.applicationId)
            encodeStringElement(descriptor, 7, value.userId)
            encodeLongElement(descriptor, 8, value.dataAccessExpirationTime.time)
            encodeNullableSerializableElement(descriptor, 9, String.serializer().nullable, value.graphDomain)
            encodeBooleanElement(descriptor, 10, value.isExpired)
            encodeBooleanElement(descriptor, 11, value.isDataAccessExpired)
            encodeBooleanElement(descriptor, 12, value.isInstagramToken)
        }
    }

    override fun deserialize(decoder: Decoder) = throw NotImplementedError()
}
