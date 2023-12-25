package org.racehorse

import android.content.Intent
import androidx.activity.ComponentActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.TapAndPayStatusCodes
import com.google.android.gms.tapandpay.issuer.IsTokenizedRequest
import com.google.android.gms.tapandpay.issuer.PushTokenizeRequest
import com.google.android.gms.tapandpay.issuer.TokenStatus
import com.google.android.gms.tapandpay.issuer.UserAddress
import com.google.android.gms.tapandpay.issuer.ViewTokenRequest
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.checkForeground
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger

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

class SerializableGooglePayUserAddress(
    val name: String,
    val address1: String,
    val address2: String,
    val locality: String,
    val administrativeArea: String,
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

class GooglePayTokenStatus(val tokenState: Int, val isSelected: Boolean) : Serializable {
    constructor(status: TokenStatus) : this(status.tokenState, status.isSelected)
}

/**
 * Get the ID of the active wallet.
 */
class GooglePayGetActiveWalletIdEvent : RequestEvent() {
    class ResultEvent(val walletId: String?) : ResponseEvent()
}

/**
 * Get the status of a token with a given token ID.
 */
class GooglePayGetTokenStatusEvent(val tokenServiceProvider: Int, val tokenId: String) : RequestEvent() {
    /**
     * @param status The token status or `null` if there's no such token.
     */
    class ResultEvent(val status: GooglePayTokenStatus?) : ResponseEvent()
}

/**
 * Get the environment (e.g. production or sandbox).
 */
class GooglePayGetEnvironmentEvent : RequestEvent() {
    /**
     * @param environment The name of the current Google Pay environment, for example: PROD, SANDBOX, or DEV.
     */
    class ResultEvent(val environment: String) : ResponseEvent()
}

/**
 * Get the stable hardware ID of the device.
 */
class GooglePayGetStableHardwareIdEvent : RequestEvent() {
    class ResultEvent(val hardwareId: String) : ResponseEvent()
}

/**
 * Get all tokens available in the wallet.
 */
class GooglePayListTokensEvent : RequestEvent() {
    class ResultEvent(val tokenInfos: List<GooglePayTokenInfo>) : ResponseEvent()
}

/**
 * Searches the wallet for a token and returns `true` if found.
 */
class GooglePayIsTokenizedEvent(
    val fpanLastFour: String,
    val network: Int,
    val tokenServiceProvider: Int
) : RequestEvent() {
    class ResultEvent(val isTokenized: Boolean) : ResponseEvent()
}

/**
 * Open Google Pay app and reveal the card.
 */
class GooglePayViewTokenEvent(val tokenId: String, val tokenServiceProvider: Int) : RequestEvent() {
    class ResultEvent(val opened: Boolean) : ResponseEvent()
}

/**
 * Posted when a wallet data has changed.
 */
class GooglePayDataChangedEvent : NoticeEvent

/**
 * Tokenize the card and push it to Google Pay.
 */
class GooglePayPushTokenizeEvent(
    val opaquePaymentCard: String,
    val displayName: String,
    val lastFour: String,
    val network: Int,
    val tokenServiceProvider: Int,
    val userAddress: SerializableGooglePayUserAddress,
) : RequestEvent() {
    class ResultEvent(val tokenId: String?) : ResponseEvent()
}

/**
 * Tokenize the card manually or resume the tokenization process it to Google Pay.
 */
class GooglePayTokenizeEvent(
    val displayName: String,
    val network: Int,
    val tokenServiceProvider: Int,
    val tokenId: String? = null,
) : RequestEvent() {
    class ResultEvent(val tokenId: String?) : ResponseEvent()
}

class GooglePayRequestSelectTokenEvent(val tokenId: String, val tokenServiceProvider: Int) : RequestEvent()

class GooglePayRequestDeleteTokenEvent(val tokenId: String, val tokenServiceProvider: Int) : RequestEvent()

class GooglePayCreateWalletEvent : RequestEvent()

/**
 * Manages tokenized cards in Google Pay.
 *
 * [Reading wallet state.](https://developers.google.com/pay/issuers/apis/push-provisioning/android/reading-wallet)
 */
open class GooglePayPlugin(
    private val activity: ComponentActivity,
    private val eventBus: EventBus = EventBus.getDefault(),
) {

    private val nextRequestCode = AtomicInteger(0x10000000)

    private val tapAndPayClient by lazy {
        TapAndPay.getClient(activity).apply {
            registerDataChangedListener {
                eventBus.post(GooglePayDataChangedEvent())
            }
        }
    }

    private val activityCallbacks = HashMap<Int, (data: Intent?) -> Unit>()

    @Subscribe
    fun onGooglePayGetActiveWalletId(event: GooglePayGetActiveWalletIdEvent) {
        tapAndPayClient.activeWalletId.addOnCompleteListener {
            event.respond {
                try {
                    GooglePayGetActiveWalletIdEvent.ResultEvent(it.getResult(ApiException::class.java))
                } catch (e: ApiException) {
                    if (e.statusCode == TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET) {
                        GooglePayGetActiveWalletIdEvent.ResultEvent(null)
                    } else {
                        ExceptionEvent(e)
                    }
                }
            }
        }
    }

    @Subscribe
    fun onGooglePayGetTokenStatus(event: GooglePayGetTokenStatusEvent) {
        tapAndPayClient.getTokenStatus(event.tokenServiceProvider, event.tokenId).addOnCompleteListener {
            event.respond {
                try {
                    GooglePayGetTokenStatusEvent.ResultEvent(GooglePayTokenStatus(it.getResult(ApiException::class.java)))
                } catch (e: ApiException) {
                    if (e.statusCode == TapAndPayStatusCodes.TAP_AND_PAY_TOKEN_NOT_FOUND) {
                        GooglePayGetTokenStatusEvent.ResultEvent(null)
                    } else {
                        ExceptionEvent(e)
                    }
                }
            }
        }
    }

    @Subscribe
    fun onGooglePayGetEnvironment(event: GooglePayGetEnvironmentEvent) {
        tapAndPayClient.environment.addOnCompleteListener {
            event.respond { GooglePayGetEnvironmentEvent.ResultEvent(it.getResult(ApiException::class.java)) }
        }
    }

    @Subscribe
    fun onGooglePayGetStableHardwareId(event: GooglePayGetStableHardwareIdEvent) {
        tapAndPayClient.stableHardwareId.addOnCompleteListener {
            event.respond { GooglePayGetStableHardwareIdEvent.ResultEvent(it.getResult(ApiException::class.java)) }
        }
    }

    @Subscribe
    fun onGooglePayListTokens(event: GooglePayListTokensEvent) {
        tapAndPayClient.listTokens().addOnCompleteListener { task ->
            event.respond {
                val tokenInfos = task.getResult(ApiException::class.java).map {
                    GooglePayTokenInfo(
                        network = it.network,
                        tokenServiceProvider = it.tokenServiceProvider,
                        tokenState = it.tokenState,
                        dpanLastFour = it.dpanLastFour,
                        fpanLastFour = it.fpanLastFour,
                        issuerName = it.issuerName,
                        issuerTokenId = it.issuerTokenId,
                        portfolioName = it.portfolioName,
                        isDefaultToken = it.isDefaultToken,
                    )
                }
                GooglePayListTokensEvent.ResultEvent(tokenInfos)
            }
        }
    }

    @Subscribe
    fun onGooglePayIsTokenized(event: GooglePayIsTokenizedEvent) {
        val request = IsTokenizedRequest.Builder()
            .setIdentifier(event.fpanLastFour)
            .setNetwork(event.network)
            .setTokenServiceProvider(event.tokenServiceProvider)
            .build()

        tapAndPayClient.isTokenized(request).addOnCompleteListener {
            event.respond { GooglePayIsTokenizedEvent.ResultEvent(it.getResult(ApiException::class.java)) }
        }
    }

    @Subscribe
    fun onGooglePayViewToken(event: GooglePayViewTokenEvent) {
        activity.checkForeground()

        val request = ViewTokenRequest.Builder()
            .setIssuerTokenId(event.tokenId)
            .setTokenServiceProvider(event.tokenServiceProvider)
            .build()

        tapAndPayClient.viewToken(request).addOnCompleteListener { task ->
            event.respond(
                GooglePayViewTokenEvent.ResultEvent(
                    try {
                        task.result.send()
                        true
                    } catch (_: Throwable) {
                        false
                    }
                )
            )
        }
    }

    @Subscribe
    fun onGooglePayPushTokenize(event: GooglePayPushTokenizeEvent) = runOperationInForeground(
        { requestCode ->
            val request = PushTokenizeRequest.Builder()
                .setOpaquePaymentCard(event.opaquePaymentCard.toByteArray())
                .setDisplayName(event.displayName)
                .setLastDigits(event.lastFour)
                .setNetwork(event.network)
                .setTokenServiceProvider(event.tokenServiceProvider)
                .setUserAddress(event.userAddress.toUserAddress())
                .build()

            tapAndPayClient.pushTokenize(activity, request, requestCode)
        },
        { event.respond(GooglePayPushTokenizeEvent.ResultEvent(it?.getStringExtra(TapAndPay.EXTRA_ISSUER_TOKEN_ID))) }
    )

    @Subscribe
    fun onGooglePayTokenize(event: GooglePayTokenizeEvent) = runOperationInForeground(
        { requestCode ->
            tapAndPayClient.tokenize(
                activity,
                event.tokenId,
                event.tokenServiceProvider,
                event.displayName,
                event.network,
                requestCode
            )
        },
        { event.respond(GooglePayTokenizeEvent.ResultEvent(it?.getStringExtra(TapAndPay.EXTRA_ISSUER_TOKEN_ID))) }
    )

    @Subscribe
    fun onGooglePayRequestSelectToken(event: GooglePayRequestSelectTokenEvent) = runOperationInForeground(
        { requestCode ->
            tapAndPayClient.requestSelectToken(
                activity,
                event.tokenId,
                event.tokenServiceProvider,
                requestCode
            )
        },
        { event.respond(VoidEvent()) }
    )

    @Subscribe
    fun onGooglePayRequestDeleteToken(event: GooglePayRequestDeleteTokenEvent) = runOperationInForeground(
        { requestCode ->
            tapAndPayClient.requestDeleteToken(
                activity,
                event.tokenId,
                event.tokenServiceProvider,
                requestCode
            )
        },
        { event.respond(VoidEvent()) }
    )

    @Subscribe
    fun onGooglePayCreateWallet(event: GooglePayCreateWalletEvent) = runOperationInForeground(
        { requestCode -> tapAndPayClient.createWallet(activity, requestCode) },
        { event.respond(VoidEvent()) }
    )

    /**
     * This method must be called from the [activity].
     */
    fun dispatchResult(requestCode: Int, @Suppress("UNUSED_PARAMETER") resultCode: Int, data: Intent?) {
        activityCallbacks.remove(requestCode)?.invoke(data)
    }

    protected open fun runOperationInForeground(
        operation: (requestCode: Int) -> Unit,
        callback: (data: Intent?) -> Unit
    ) {
        activity.checkForeground()

        val requestCode = nextRequestCode.getAndIncrement()
        operation(requestCode)
        activityCallbacks[requestCode] = callback
    }
}
