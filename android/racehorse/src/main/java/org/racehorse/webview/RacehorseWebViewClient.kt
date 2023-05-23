package org.racehorse.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.view.KeyEvent
import android.webkit.ClientCertRequest
import android.webkit.HttpAuthHandler
import android.webkit.RenderProcessGoneDetail
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import org.greenrobot.eventbus.EventBus
import org.racehorse.utils.SyncHandlerEvent
import org.racehorse.utils.postForSubscriber
import org.racehorse.utils.postForSyncHandler

/**
 * Handlers of this event must be called on the posting thread, and should update the [response] field.
 */
class ShouldInterceptRequestEvent(
    val view: WebView,
    val request: WebResourceRequest,
    var response: WebResourceResponse?
)

class ShouldOverrideKeyEventEvent(val view: WebView, val event: KeyEvent) : SyncHandlerEvent()

class ShouldOverrideUrlLoadingEvent(val view: WebView, val request: WebResourceRequest) : SyncHandlerEvent()

class PageStartedEvent(val view: WebView, val url: String, val favicon: Bitmap?)

class PageFinishedEvent(val view: WebView, val url: String)

class LoadResourceEvent(val view: WebView, val url: String)

class PageCommitVisibleEvent(val view: WebView, val url: String)

class ReceivedErrorEvent(val view: WebView, val request: WebResourceRequest, val error: WebResourceError)

class ReceivedHttpErrorEvent(val view: WebView, val request: WebResourceRequest, val errorResponse: WebResourceResponse)

class FormResubmissionEvent(val view: WebView, val dontResend: Message, val resend: Message) : SyncHandlerEvent()

class UpdateVisitedHistoryEvent(val view: WebView, val url: String, val isReload: Boolean)

class ReceivedSslErrorEvent(val view: WebView, val handler: SslErrorHandler, val error: SslError) : SyncHandlerEvent()

class ReceivedClientCertRequestEvent(val view: WebView, val request: ClientCertRequest) : SyncHandlerEvent()

class ReceivedHttpAuthRequestEvent(
    val view: WebView,
    val handler: HttpAuthHandler,
    val host: String,
    val realm: String
) : SyncHandlerEvent()

class UnhandledKeyEventEvent(val view: WebView, val event: KeyEvent)

class ScaleChangedEvent(val view: WebView, val oldScale: Float, val newScale: Float)

class ReceivedLoginRequestEvent(val view: WebView, val realm: String, val account: String?, val args: String)

class RenderProcessGoneEvent(val view: WebView, val detail: RenderProcessGoneDetail) : SyncHandlerEvent()

class SafeBrowsingHitEvent(
    val view: WebView,
    val request: WebResourceRequest,
    val threatType: Int,
    val callback: SafeBrowsingResponse
)

open class RacehorseWebViewClient(val eventBus: EventBus = EventBus.getDefault()) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return eventBus.postForSubscriber { ShouldInterceptRequestEvent(view, request, null) }?.response
    }

    override fun shouldOverrideKeyEvent(view: WebView, event: KeyEvent): Boolean {
        return eventBus.postForSyncHandler { ShouldOverrideKeyEventEvent(view, event) }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return eventBus.postForSyncHandler { ShouldOverrideUrlLoadingEvent(view, request) }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        eventBus.postForSubscriber { PageStartedEvent(view, url, favicon) }
    }

    override fun onPageFinished(view: WebView, url: String) {
        eventBus.postForSubscriber { PageFinishedEvent(view, url) }
    }

    override fun onLoadResource(view: WebView, url: String) {
        eventBus.postForSubscriber { LoadResourceEvent(view, url) }
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        eventBus.postForSubscriber { PageCommitVisibleEvent(view, url) }
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        eventBus.postForSubscriber { ReceivedErrorEvent(view, request, error) }
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        eventBus.postForSubscriber { ReceivedHttpErrorEvent(view, request, errorResponse) }
    }

    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        if (!eventBus.postForSyncHandler { FormResubmissionEvent(view, dontResend, resend) }) {
            super.onFormResubmission(view, dontResend, resend)
        }
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        eventBus.postForSubscriber { UpdateVisitedHistoryEvent(view, url, isReload) }
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        if (!eventBus.postForSyncHandler { ReceivedSslErrorEvent(view, handler, error) }) {
            super.onReceivedSslError(view, handler, error)
        }
    }

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        if (!eventBus.postForSyncHandler { ReceivedClientCertRequestEvent(view, request) }) {
            super.onReceivedClientCertRequest(view, request)
        }
    }

    override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
        if (!eventBus.postForSyncHandler { ReceivedHttpAuthRequestEvent(view, handler, host, realm) }) {
            super.onReceivedHttpAuthRequest(view, handler, host, realm)
        }
    }

    override fun onUnhandledKeyEvent(view: WebView, event: KeyEvent) {
        eventBus.postForSubscriber { UnhandledKeyEventEvent(view, event) }
    }

    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        eventBus.postForSubscriber { ScaleChangedEvent(view, oldScale, newScale) }
    }

    override fun onReceivedLoginRequest(view: WebView, realm: String, account: String?, args: String) {
        eventBus.postForSubscriber { ReceivedLoginRequestEvent(view, realm, account, args) }
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        return eventBus.postForSyncHandler { RenderProcessGoneEvent(view, detail) }
    }

    override fun onSafeBrowsingHit(
        view: WebView,
        request: WebResourceRequest,
        threatType: Int,
        callback: SafeBrowsingResponse
    ) {
        eventBus.postForSubscriber { SafeBrowsingHitEvent(view, request, threatType, callback) }
    }
}
