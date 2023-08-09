package org.racehorse

import android.content.Intent
import androidx.activity.ComponentActivity
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.issuer.IsTokenizedRequest
import com.google.android.gms.tapandpay.issuer.PushTokenizeRequest
import com.google.android.gms.tapandpay.issuer.UserAddress
import com.google.android.gms.tapandpay.issuer.ViewTokenRequest
import org.greenrobot.eventbus.Subscribe
import java.io.Serializable

class GooglePayTokenInfo(
    val network: Int,
    val tokenServiceProvider: Int,
    val tokenState: Int,
    val dpanLastFour: String,
    val fpanLastFour: String,
    val issuerName: String,
    val issuerTokenId: String,
    val portfolioName: String,
    val isDefaultToken: Boolean,
) : Serializable

class SerializableUserAddress(
    val name: String,
    val address1: String,
    val address2: String,
    val locality: String, // Mountain View
    val administrativeArea: String, // CA
    val countryCode: String,
    val postalCode: String,
    val phoneNumber: String,
) : Serializable {
    fun toUserAddress() = UserAddress.newBuilder()
        .setName(name)
        .setAddress1(address1)
        .setAddress2(address2)
        .setLocality(locality)
        .setAdministrativeArea(administrativeArea)
        .setCountryCode(countryCode)
        .setPostalCode(postalCode)
        .setPhoneNumber(phoneNumber)
        .build()
}

/**
 * Detect current environment.
 */
class GetGooglePayEnvironmentEvent : RequestEvent() {
    class ResultEvent(
        /**
         * The name of the current Google Pay environment, for example: PROD, SANDBOX, or DEV.
         */
        val environment: String?
    ) : ResponseEvent()
}

/**
 * Get all tokens available in the wallet.
 */
class GetGooglePayTokensEvent : RequestEvent() {
    class ResultEvent(val tokenInfos: Array<GooglePayTokenInfo>) : ResponseEvent()
}

/**
 * Open Google Pay app and reveal the card.
 */
class OpenTokenInGooglePayEvent(
    val issuerTokenId: String,
    val tokenServiceProvider: Int
) : RequestEvent() {
    class ResultEvent(val opened: Boolean) : ResponseEvent()
}

/**
 * Check that the card was provisioned to Google Pay.
 */
class IsTokenizedInGooglePayEvent(
    val fpanLastFour: String,
    val network: Int,
    val tokenServiceProvider: Int
) : RequestEvent() {
    class ResultEvent(val isTokenized: Boolean) : ResponseEvent()
}

/**
 * Tokenize the card and push it to Google Pay.
 */
class PushToGooglePayEvent(
    val opaquePaymentCard: String,
    val displayName: String,
    val lastFour: String,
    val network: Int,
    val tokenServiceProvider: Int,
    val userAddress: SerializableUserAddress,
) : RequestEvent() {
    class ResultEvent(val tokenId: String?) : ResponseEvent()
}

/**
 * Manages tokenized cards in Google Pay.
 *
 * [Reading wallet state.](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet)
 */
class GooglePayPlugin(private val activity: ComponentActivity) {

    companion object {
        const val REQUEST_CODE_PUSH_TO_GOOGLE_PAY = 378204261
    }

    private val tapAndPayClient by lazy { TapAndPay.getClient(activity) }

    /**
     * If not `null` then card provisioning is in progress.
     */
    private var pendingPushToGooglePayEvent: PushToGooglePayEvent? = null

    @Subscribe
    fun onGetGooglePayEnvironment(event: GetGooglePayEnvironmentEvent) {
        tapAndPayClient.environment.addOnCompleteListener {
            event.respond(GetGooglePayEnvironmentEvent.ResultEvent(if (it.isSuccessful) it.result else null))
        }
    }

    @Subscribe
    fun onGetGooglePayTokens(event: GetGooglePayTokensEvent) {
        tapAndPayClient.listTokens().addOnCompleteListener { task ->
            event.respond(
                GetGooglePayTokensEvent.ResultEvent(
                    if (task.isSuccessful) {
                        task.result.map {
                            GooglePayTokenInfo(
                                it.network,
                                it.tokenServiceProvider,
                                it.tokenState,
                                it.dpanLastFour,
                                it.fpanLastFour,
                                it.issuerName,
                                it.issuerTokenId,
                                it.portfolioName,
                                it.isDefaultToken,
                            )
                        }.toTypedArray()
                    } else arrayOf()
                )
            )
        }
    }

    @Subscribe
    fun onOpenTokenInGooglePay(event: OpenTokenInGooglePayEvent) {
        val request = ViewTokenRequest.Builder()
            .setIssuerTokenId(event.issuerTokenId)
            .setTokenServiceProvider(event.tokenServiceProvider)
            .build()

        tapAndPayClient.viewToken(request).addOnCompleteListener {
            event.respond(
                OpenTokenInGooglePayEvent.ResultEvent(
                    if (it.isSuccessful) {
                        it.result.send()
                        true
                    } else {
                        false
                    }
                )
            )
        }
    }

    @Subscribe
    fun onIsTokenizedInGooglePay(event: IsTokenizedInGooglePayEvent) {
        val request = IsTokenizedRequest.Builder()
            .setIdentifier(event.fpanLastFour)
            .setNetwork(event.network)
            .setTokenServiceProvider(event.tokenServiceProvider)
            .build()

        tapAndPayClient.isTokenized(request).addOnCompleteListener {
            event.respond(IsTokenizedInGooglePayEvent.ResultEvent(it.isSuccessful && it.result))
        }
    }

    /**
     * [Sequence diagrams for Android push provisioning options](https://developers.google.com/pay/issuers/apis/push-provisioning/android/integration-steps)
     */
    @Subscribe
    fun onPushToGooglePay(event: PushToGooglePayEvent) {
        check(pendingPushToGooglePayEvent == null)

        val request = PushTokenizeRequest.Builder()
            .setOpaquePaymentCard(event.opaquePaymentCard.toByteArray())
            .setDisplayName(event.displayName)
            .setLastDigits(event.lastFour)
            .setNetwork(event.network)
            .setTokenServiceProvider(event.tokenServiceProvider)
            .setUserAddress(event.userAddress.toUserAddress())
            .build()

        pendingPushToGooglePayEvent = event
        try {
            tapAndPayClient.pushTokenize(activity, request, REQUEST_CODE_PUSH_TO_GOOGLE_PAY)
        } catch (e: Throwable) {
            e.printStackTrace()

            pendingPushToGooglePayEvent = null
            event.respond(ExceptionEvent(e))
        }
    }

    fun onPushToGooglePayResult(resultCode: Int, data: Intent?) {
        pendingPushToGooglePayEvent?.respond(PushToGooglePayEvent.ResultEvent(data?.getStringExtra(TapAndPay.EXTRA_ISSUER_TOKEN_ID)))
        pendingPushToGooglePayEvent = null
    }
}
