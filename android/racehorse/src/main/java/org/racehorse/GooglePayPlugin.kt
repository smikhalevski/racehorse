@file:UseSerializers(UserAddressSerializer::class, TokenStatusSerializer::class, TokenInfoSerializer::class)

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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.apiResult
import org.racehorse.utils.decodeNullableStringElement
import java.util.concurrent.atomic.AtomicInteger

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
    class ResultEvent(val status: TokenStatus?) : ResponseEvent()
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
    class ResultEvent(val tokenInfos: List<TokenInfo>) : ResponseEvent()
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
    val userAddress: UserAddress?,
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
                        it.apiResult
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
                        it.apiResult
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
                .setUserAddress(event.userAddress ?: UserAddress.newBuilder().build())

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
        callback = { event.respond(VoidEvent) }
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
        callback = { event.respond(VoidEvent) }
    )

    @Subscribe
    fun onGooglePayCreateWallet(event: GooglePayCreateWalletEvent) = runOperation(
        operation = { requestCode -> tapAndPayClient.createWallet(activity, requestCode) },
        callback = { event.respond(VoidEvent) }
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

object UserAddressSerializer : KSerializer<UserAddress> {
    override val descriptor = buildClassSerialDescriptor(UserAddress::class.java.simpleName) {
        element<String?>("name", isOptional = true)
        element<String?>("address1", isOptional = true)
        element<String?>("address2", isOptional = true)
        element<String?>("locality", isOptional = true)
        element<String?>("administrativeArea", isOptional = true)
        element<String?>("countryCode", isOptional = true)
        element<String?>("postalCode", isOptional = true)
        element<String?>("phoneNumber", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: UserAddress) = throw NotImplementedError()

    override fun deserialize(decoder: Decoder) = decoder.decodeStructure(descriptor) {
        val builder = UserAddress.newBuilder()
            .setName("")
            .setAddress1("")
            .setAddress2("")
            .setLocality("")
            .setAdministrativeArea("")
            .setCountryCode("")
            .setPostalCode("")
            .setPhoneNumber("")

        while (true) {
            when (val index = decodeElementIndex(descriptor)) {
                0 -> builder.setName(decodeNullableStringElement(descriptor, index).orEmpty())
                1 -> builder.setAddress1(decodeNullableStringElement(descriptor, index).orEmpty())
                2 -> builder.setAddress2(decodeNullableStringElement(descriptor, index).orEmpty())
                3 -> builder.setLocality(decodeNullableStringElement(descriptor, index).orEmpty())
                4 -> builder.setAdministrativeArea(decodeNullableStringElement(descriptor, index).orEmpty())
                5 -> builder.setCountryCode(decodeNullableStringElement(descriptor, index).orEmpty())
                6 -> builder.setPostalCode(decodeNullableStringElement(descriptor, index).orEmpty())
                7 -> builder.setPhoneNumber(decodeNullableStringElement(descriptor, index).orEmpty())
                DECODE_DONE -> break
            }
        }

        builder.build()
    }
}

object TokenStatusSerializer : KSerializer<TokenStatus> {
    override val descriptor = buildClassSerialDescriptor(TokenStatus::class.java.simpleName) {
        element<Int>("tokenState")
        element<Boolean>("isSelected")
    }

    override fun serialize(encoder: Encoder, value: TokenStatus) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.tokenState)
            encodeBooleanElement(descriptor, 1, value.isSelected)
        }
    }

    override fun deserialize(decoder: Decoder) = throw NotImplementedError()
}

object TokenInfoSerializer : KSerializer<TokenInfo> {
    override val descriptor = buildClassSerialDescriptor(TokenInfo::class.java.simpleName) {
        element<Int>("network")
        element<Int>("tokenServiceProvider")
        element<Int>("tokenState")
        element<String>("dpanLastFour")
        element<String>("fpanLastFour")
        element<String>("issuerName")
        element<String>("issuerTokenId")
        element<String>("portfolioName")
        element<Boolean>("isDefaultToken")
    }

    override fun serialize(encoder: Encoder, value: TokenInfo) {
        encoder.encodeStructure(descriptor) {
            encodeIntElement(descriptor, 0, value.network)
            encodeIntElement(descriptor, 1, value.tokenServiceProvider)
            encodeIntElement(descriptor, 2, value.tokenState)
            encodeStringElement(descriptor, 3, value.dpanLastFour)
            encodeStringElement(descriptor, 4, value.fpanLastFour)
            encodeStringElement(descriptor, 5, value.issuerName)
            encodeStringElement(descriptor, 6, value.issuerTokenId)
            encodeStringElement(descriptor, 7, value.portfolioName)
            encodeBooleanElement(descriptor, 8, value.isDefaultToken)
        }
    }

    override fun deserialize(decoder: Decoder) = throw NotImplementedError()
}
