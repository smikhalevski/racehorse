package org.racehorse.webview

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.view.KeyEvent
import android.webkit.*
import org.greenrobot.eventbus.EventBus
import org.racehorse.utils.HandlerEvent
import org.racehorse.utils.postForHandler
import org.racehorse.utils.postForSubscriber

class SynchronousResult<T>(var value: T)

class ShouldInterceptRequestEvent(
    val view: WebView,
    val request: WebResourceRequest,
    val result: SynchronousResult<WebResourceResponse?>
)

class ShouldOverrideKeyEventEvent(val view: WebView, val event: KeyEvent) : HandlerEvent()

class ShouldOverrideUrlLoadingEvent(val view: WebView, val request: WebResourceRequest) : HandlerEvent()

class PageStartedEvent(val view: WebView, val url: String, val favicon: Bitmap?)

class PageFinishedEvent(val view: WebView, val url: String)

class LoadResourceEvent(val view: WebView, val url: String)

class PageCommitVisibleEvent(val view: WebView, val url: String)

class ReceivedErrorEvent(val view: WebView, val request: WebResourceRequest, val error: WebResourceError)

class ReceivedHttpErrorEvent(val view: WebView, val request: WebResourceRequest, val errorResponse: WebResourceResponse)

class FormResubmissionEvent(val view: WebView, val dontResend: Message, val resend: Message) : HandlerEvent()

class UpdateVisitedHistoryEvent(val view: WebView, val url: String, val isReload: Boolean)

class ReceivedSslErrorEvent(val view: WebView, val handler: SslErrorHandler, val error: SslError) : HandlerEvent()

class ReceivedClientCertRequestEvent(val view: WebView, val request: ClientCertRequest) : HandlerEvent()

class ReceivedHttpAuthRequestEvent(
    val view: WebView,
    val handler: HttpAuthHandler,
    val host: String,
    val realm: String
) : HandlerEvent()

class UnhandledKeyEventEvent(val view: WebView, val event: KeyEvent)

class ScaleChangedEvent(val view: WebView, val oldScale: Float, val newScale: Float)

class ReceivedLoginRequestEvent(val view: WebView, val realm: String, val account: String?, val args: String)

class RenderProcessGoneEvent(val view: WebView, val detail: RenderProcessGoneDetail) : HandlerEvent()

class SafeBrowsingHitEvent(
    val view: WebView,
    val request: WebResourceRequest,
    val threatType: Int,
    val callback: SafeBrowsingResponse
) : HandlerEvent()

open class RacehorseWebViewClient(val eventBus: EventBus = EventBus.getDefault()) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
        SynchronousResult<WebResourceResponse?>(null).let {
            eventBus.postForSubscriber { ShouldInterceptRequestEvent(view, request, it) }
            it.value
        }

    override fun shouldOverrideKeyEvent(view: WebView, event: KeyEvent) =
        eventBus.postForHandler { ShouldOverrideKeyEventEvent(view, event) }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) =
        eventBus.postForHandler { ShouldOverrideUrlLoadingEvent(view, request) }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) =
        eventBus.postForSubscriber { PageStartedEvent(view, url, favicon) }

    override fun onPageFinished(view: WebView, url: String) =
        eventBus.postForSubscriber { PageFinishedEvent(view, url) }

    override fun onLoadResource(view: WebView, url: String) =
        eventBus.postForSubscriber { LoadResourceEvent(view, url) }

    override fun onPageCommitVisible(view: WebView, url: String) =
        eventBus.postForSubscriber { PageCommitVisibleEvent(view, url) }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) =
        eventBus.postForSubscriber { ReceivedErrorEvent(view, request, error) }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) =
        eventBus.postForSubscriber { ReceivedHttpErrorEvent(view, request, errorResponse) }

    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        if (!eventBus.postForHandler { FormResubmissionEvent(view, dontResend, resend) }) {
            dontResend.sendToTarget()
        }
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) =
        eventBus.postForSubscriber { UpdateVisitedHistoryEvent(view, url, isReload) }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        if (!eventBus.postForHandler { ReceivedSslErrorEvent(view, handler, error) }) {
            handler.cancel()
        }
    }

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        if (!eventBus.postForHandler { ReceivedClientCertRequestEvent(view, request) }) {
            request.cancel()
        }
    }

    override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
        if (!eventBus.postForHandler { ReceivedHttpAuthRequestEvent(view, handler, host, realm) }) {
            handler.cancel()
        }
    }

    override fun onUnhandledKeyEvent(view: WebView, event: KeyEvent) =
        eventBus.postForSubscriber { UnhandledKeyEventEvent(view, event) }

    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) =
        eventBus.postForSubscriber { ScaleChangedEvent(view, oldScale, newScale) }

    override fun onReceivedLoginRequest(view: WebView, realm: String, account: String?, args: String) =
        eventBus.postForSubscriber { ReceivedLoginRequestEvent(view, realm, account, args) }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail) =
        eventBus.postForHandler { RenderProcessGoneEvent(view, detail) }

    override fun onSafeBrowsingHit(
        view: WebView,
        request: WebResourceRequest,
        threatType: Int,
        callback: SafeBrowsingResponse
    ) = eventBus.postForSubscriber { SafeBrowsingHitEvent(view, request, threatType, callback) }
}
