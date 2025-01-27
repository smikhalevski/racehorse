package org.racehorse

import android.content.Intent
import androidx.activity.ComponentActivity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tapandpay.TapAndPay
import com.google.android.gms.tapandpay.TapAndPayStatusCodes
import com.google.android.gms.tapandpay.issuer.IsTokenizedRequest
import com.google.android.gms.tapandpay.issuer.PushTokenizeRequest
import com.google.android.gms.tapandpay.issuer.TokenInfo
import com.google.android.gms.tapandpay.issuer.TokenStatus
import com.google.android.gms.tapandpay.issuer.UserAddress
import com.google.android.gms.tapandpay.issuer.ViewTokenRequest
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.apiResult
import java.util.concurrent.atomic.AtomicInteger

@Serializable
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
) {
    constructor(tokenInfo: TokenInfo) : this(
        network = tokenInfo.network,
        tokenServiceProvider = tokenInfo.tokenServiceProvider,
        tokenState = tokenInfo.tokenState,
        dpanLastFour = tokenInfo.dpanLastFour,
        fpanLastFour = tokenInfo.fpanLastFour,
        issuerName = tokenInfo.issuerName,
        issuerTokenId = tokenInfo.issuerTokenId,
        portfolioName = tokenInfo.portfolioName,
        isDefaultToken = tokenInfo.isDefaultToken,
    )
}

@Serializable
class SerializableGooglePayUserAddress(
    val name: String?,
    val address1: String?,
    val address2: String?,
    val locality: String?,
    val administrativeArea: String?,
    val countryCode: String?,
    val postalCode: String?,
    val phoneNumber: String?,
) {
    fun toUserAddress() = UserAddress.newBuilder()
        .setName(name.orEmpty())
        .setAddress1(address1.orEmpty())
        .setAddress2(address2.orEmpty())
        .setLocality(locality.orEmpty())
        .setAdministrativeArea(administrativeArea.orEmpty())
        .setCountryCode(countryCode.orEmpty())
        .setPostalCode(postalCode.orEmpty())
        .setPhoneNumber(phoneNumber.orEmpty())
        .build()
}

@Serializable
class GooglePayTokenStatus(val tokenState: Int, val isSelected: Boolean) {
    constructor(status: TokenStatus) : this(status.tokenState, status.isSelected)
}

/**
 * Get the ID of the active wallet.
 */
@Serializable
class GooglePayGetActiveWalletIdEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val walletId: String?) : ResponseEvent()
}

/**
 * Get the status of a token with a given token ID.
 */
@Serializable
class GooglePayGetTokenStatusEvent(val tokenServiceProvider: Int, val tokenId: String) : RequestEvent() {

    /**
     * @param status The token status or `null` if there's no such token.
     */
    @Serializable
    class ResultEvent(val status: GooglePayTokenStatus?) : ResponseEvent()
}

/**
 * Get the environment (e.g. production or sandbox).
 */
@Serializable
class GooglePayGetEnvironmentEvent : RequestEvent() {

    /**
     * @param environment The name of the current Google Pay environment, for example: PROD, SANDBOX, or DEV.
     */
    @Serializable
    class ResultEvent(val environment: String) : ResponseEvent()
}

/**
 * Get the stable hardware ID of the device.
 */
@Serializable
class GooglePayGetStableHardwareIdEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val hardwareId: String) : ResponseEvent()
}

/**
 * Get all tokens available in the wallet.
 */
@Serializable
class GooglePayListTokensEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val tokenInfos: List<GooglePayTokenInfo>) : ResponseEvent()
}

/**
 * Searches the wallet for a token and returns `true` if found.
 */
@Serializable
class GooglePayIsTokenizedEvent(
    val fpanLastFour: String,
    val network: Int,
    val tokenServiceProvider: Int
) : RequestEvent() {

    @Serializable
    class ResultEvent(val isTokenized: Boolean) : ResponseEvent()
}

/**
 * Open Google Pay app and reveal the card.
 */
@Serializable
class GooglePayViewTokenEvent(val tokenId: String, val tokenServiceProvider: Int) : RequestEvent() {

    @Serializable
    class ResultEvent(val isOpened: Boolean) : ResponseEvent()
}

/**
 * Posted when a wallet data has changed.
 */
@Serializable
class GooglePayDataChangedEvent : NoticeEvent

/**
 * Tokenize the card and push it to Google Pay.
 */
@Serializable
class GooglePayPushTokenizeEvent(
    val opaquePaymentCard: String,
    val displayName: String,
    val lastFour: String,
    val network: Int,
    val tokenServiceProvider: Int,
    val userAddress: SerializableGooglePayUserAddress?,
) : RequestEvent() {

    @Serializable
    class ResultEvent(val tokenId: String?) : ResponseEvent()
}

/**
 * Tokenize the card manually or resume the tokenization process in Google Pay.
 */
@Serializable
class GooglePayTokenizeEvent(
    val displayName: String,
    val network: Int,
    val tokenServiceProvider: Int,
    val tokenId: String? = null,
) : RequestEvent() {

    @Serializable
    class ResultEvent(val tokenId: String?) : ResponseEvent()
}

@Serializable
class GooglePayRequestSelectTokenEvent(val tokenId: String, val tokenServiceProvider: Int) : RequestEvent()

@Serializable
class GooglePayRequestDeleteTokenEvent(val tokenId: String, val tokenServiceProvider: Int) : RequestEvent()

@Serializable
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
                GooglePayGetActiveWalletIdEvent.ResultEvent(
                    try {
                        it.apiResult
                    } catch (e: ApiException) {
                        if (e.statusCode == TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET) null else throw e
                    }
                )
            }
        }
    }

    @Subscribe
    fun onGooglePayGetTokenStatus(event: GooglePayGetTokenStatusEvent) {
        tapAndPayClient.getTokenStatus(event.tokenServiceProvider, event.tokenId).addOnCompleteListener {
            event.respond {
                GooglePayGetTokenStatusEvent.ResultEvent(
                    try {
                        GooglePayTokenStatus(it.apiResult)
                    } catch (e: ApiException) {
                        if (
                            e.statusCode == TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET ||
                            e.statusCode == TapAndPayStatusCodes.TAP_AND_PAY_TOKEN_NOT_FOUND
                        ) null else throw e
                    }
                )
            }
        }
    }

    @Subscribe
    fun onGooglePayGetEnvironment(event: GooglePayGetEnvironmentEvent) {
        tapAndPayClient.environment.addOnCompleteListener {
            event.respond { GooglePayGetEnvironmentEvent.ResultEvent(it.apiResult) }
        }
    }

    @Subscribe
    fun onGooglePayGetStableHardwareId(event: GooglePayGetStableHardwareIdEvent) {
        tapAndPayClient.stableHardwareId.addOnCompleteListener {
            event.respond { GooglePayGetStableHardwareIdEvent.ResultEvent(it.apiResult) }
        }
    }

    @Subscribe
    fun onGooglePayListTokens(event: GooglePayListTokensEvent) {
        tapAndPayClient.listTokens().addOnCompleteListener {
            event.respond {
                GooglePayListTokensEvent.ResultEvent(
                    try {
                        it.apiResult.map(::GooglePayTokenInfo)
                    } catch (e: ApiException) {
                        if (e.statusCode == TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET) emptyList() else throw e
                    }
                )
            }
        }
    }

    @Subscribe
    fun onGooglePayIsTokenized(event: GooglePayIsTokenizedEvent) {
        val builder = IsTokenizedRequest.Builder()
            .setIdentifier(event.fpanLastFour)
            .setNetwork(event.network)
            .setTokenServiceProvider(event.tokenServiceProvider)

        tapAndPayClient.isTokenized(builder.build()).addOnCompleteListener {
            event.respond {
                GooglePayIsTokenizedEvent.ResultEvent(
                    try {
                        it.apiResult
                    } catch (e: ApiException) {
                        if (e.statusCode == TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET) false else throw e
                    }
                )
            }
        }
    }

    @Subscribe
    fun onGooglePayViewToken(event: GooglePayViewTokenEvent) {
        val builder = ViewTokenRequest.Builder()
            .setIssuerTokenId(event.tokenId)
            .setTokenServiceProvider(event.tokenServiceProvider)

        tapAndPayClient.viewToken(builder.build()).addOnCompleteListener {
            event.respond(
                GooglePayViewTokenEvent.ResultEvent(
                    try {
                        it.apiResult.send()
                        true
                    } catch (_: Throwable) {
                        false
                    }
                )
            )
        }
    }

    @Subscribe
    fun onGooglePayPushTokenize(event: GooglePayPushTokenizeEvent) = runOperation(
        operation = { requestCode ->
            val builder = PushTokenizeRequest.Builder()
                .setOpaquePaymentCard(event.opaquePaymentCard.toByteArray())
                .setDisplayName(event.displayName)
                .setLastDigits(event.lastFour)
                .setNetwork(event.network)
                .setTokenServiceProvider(event.tokenServiceProvider)
                .setUserAddress(event.userAddress?.toUserAddress() ?: UserAddress.newBuilder().build())

            tapAndPayClient.pushTokenize(activity, builder.build(), requestCode)
        },
        callback = { event.respond(GooglePayPushTokenizeEvent.ResultEvent(it?.getStringExtra(TapAndPay.EXTRA_ISSUER_TOKEN_ID))) }
    )

    @Subscribe
    fun onGooglePayTokenize(event: GooglePayTokenizeEvent) = runOperation(
        operation = { requestCode ->
            tapAndPayClient.tokenize(
                activity,
                event.tokenId,
                event.tokenServiceProvider,
                event.displayName,
                event.network,
                requestCode
            )
        },
        callback = { event.respond(GooglePayTokenizeEvent.ResultEvent(it?.getStringExtra(TapAndPay.EXTRA_ISSUER_TOKEN_ID))) }
    )

    @Subscribe
    fun onGooglePayRequestSelectToken(event: GooglePayRequestSelectTokenEvent) = runOperation(
        operation = { requestCode ->
            tapAndPayClient.requestSelectToken(
                activity,
                event.tokenId,
                event.tokenServiceProvider,
                requestCode
            )
        },
        callback = { event.respond(VoidEvent()) }
    )

    @Subscribe
    fun onGooglePayRequestDeleteToken(event: GooglePayRequestDeleteTokenEvent) = runOperation(
        operation = { requestCode ->
            tapAndPayClient.requestDeleteToken(
                activity,
                event.tokenId,
                event.tokenServiceProvider,
                requestCode
            )
        },
        callback = { event.respond(VoidEvent()) }
    )

    @Subscribe
    fun onGooglePayCreateWallet(event: GooglePayCreateWalletEvent) = runOperation(
        operation = { requestCode -> tapAndPayClient.createWallet(activity, requestCode) },
        callback = { event.respond(VoidEvent()) }
    )

    /**
     * This method must be called from the [activity].
     */
    fun dispatchResult(requestCode: Int, @Suppress("UNUSED_PARAMETER") resultCode: Int, data: Intent?) {
        activityCallbacks.remove(requestCode)?.invoke(data)
    }

    private fun runOperation(
        operation: (requestCode: Int) -> Unit,
        callback: (data: Intent?) -> Unit
    ) {
        val requestCode = nextRequestCode.getAndIncrement()

        activityCallbacks[requestCode] = callback

        try {
            operation(requestCode)
        } catch (e: Throwable) {
            activityCallbacks.remove(requestCode)
            throw e
        }
    }
}
