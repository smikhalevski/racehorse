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
import org.racehorse.eventbus.RacehorseConnection
import org.racehorse.utils.SyncHandlerEvent

/**
 * Notify the host application of a resource request and allow the
 * application to return the data.  If the return value is `null`, the WebView
 * will continue to load the resource as usual.  Otherwise, the return
 * response and data will be used.
 *
 * This callback is invoked for a variety of URL schemes (e.g., `http(s):`, `data:`,
 * `file:`, etc.), not only those schemes which send requests over the network.
 * This is not called for `javascript:` URLs, `blob:` URLs, or for assets accessed
 * via `file:///android_asset/` or `file:///android_res/` URLs.
 *
 * In the case of redirects, this is only called for the initial resource URL, not any
 * subsequent redirect URLs.
 *
 * **Note:** This method is called on a thread
 * other than the UI thread so clients should exercise caution
 * when accessing private data or the view system.
 *
 * **Note:** When Safe Browsing is enabled, these URLs still undergo Safe
 * Browsing checks. If this is undesired, you can use [WebView.setSafeBrowsingWhitelist]
 * to skip Safe Browsing checks for that host or dismiss the warning in [SafeBrowsingHitEvent]
 * by calling [SafeBrowsingResponse.proceed].
 *
 * Assign a [WebResourceResponse] containing the response information to [response] or `null` if the
 * WebView should load the resource itself.
 */
class ShouldInterceptRequestEvent(
    /**
     * The [WebView] that is requesting the resource.
     */
    val view: WebView,

    /**
     * Object containing the details of the request.
     */
    val request: WebResourceRequest,

    /**
     * Mutable response.
     */
    var response: WebResourceResponse?
)

/**
 * Give the host application a chance to handle the key event synchronously.
 * e.g. menu shortcut key events need to be filtered this way. If return
 * true, WebView will not handle the key event. If return `false`, WebView
 * will always handle the key event, so none of the super in the view chain
 * will see the key event. The default behavior returns `false`.
 *
 * Call [SyncHandlerEvent.shouldHandle] if the host application wants to handle
 * the key event itself.
 */
class ShouldOverrideKeyEventEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * The key event.
     */
    val event: KeyEvent
) : SyncHandlerEvent()

/**
 * Give the host application a chance to take control when a URL is about to be loaded in the
 * current WebView. If a WebViewClient is not provided, by default WebView will ask Activity
 * Manager to choose the proper handler for the URL. If a WebViewClient is provided, returning
 * `true` causes the current WebView to abort loading the URL, while returning
 * `false` causes the WebView to continue loading the URL as usual.
 *
 * **Note:** Do not call [WebView.loadUrl] with the request's
 * URL and then return `true`. This unnecessarily cancels the current load and starts a
 * new load with the same URL. The correct way to continue loading a given URL is to simply
 * return `false`, without calling [WebView.loadUrl].
 *
 * **Note:** This method is not called for POST requests.
 *
 * **Note:** This method may be called for subframes and with non-HTTP(S)
 * schemes; calling [WebView.loadUrl] with such a URL will fail.
 */
class ShouldOverrideUrlLoadingEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * Object containing the details of the request.
     */
    val request: WebResourceRequest
) : SyncHandlerEvent()

/**
 * Notify the host application that a page has started loading. This method
 * is called once for each main frame load so a page with iframes or
 * framesets will call onPageStarted one time for the main frame. This also
 * means that onPageStarted will not be called when the contents of an
 * embedded frame changes, i.e. clicking a link whose target is an iframe,
 * it will also not be called for fragment navigations (navigations to
 * #fragment_id).
 */
class PageStartedEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * The url to be loaded.
     */
    val url: String,

    /**
     * The favicon for this page if it already exists in the database.
     */
    val favicon: Bitmap?
)

/**
 * Notify the host application that a page has finished loading. This method
 * is called only for main frame. Receiving an `onPageFinished()` callback does not
 * guarantee that the next frame drawn by WebView will reflect the state of the DOM at this
 * point. In order to be notified that the current DOM state is ready to be rendered, request a
 * visual state callback with [WebView.postVisualStateCallback] and wait for the supplied
 * callback to be triggered.
 */
class PageFinishedEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * The url of the page.
     */
    val url: String
)

/**
 * Notify the host application that the WebView will load the resource
 * specified by the given url.
 */
class LoadResourceEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * The url of the resource the WebView will load.
     */
    val url: String
)

/**
 * Notify the host application that [WebView] content left over from
 * previous page navigations will no longer be drawn.
 *
 * This callback can be used to determine the point at which it is safe to make a recycled
 * [WebView] visible, ensuring that no stale content is shown. It is called
 * at the earliest point at which it can be guaranteed that [WebView.onDraw] will no
 * longer draw any content from previous navigations. The next draw will display either the
 * [WebView.setBackgroundColor] of the [WebView], or some of the
 * contents of the newly loaded page.
 *
 * This method is called when the body of the HTTP response has started loading, is reflected
 * in the DOM, and will be visible in subsequent draws. This callback occurs early in the
 * document loading process, and as such you should expect that linked resources (for example,
 * CSS and images) may not be available.
 *
 * For more fine-grained notification of visual state updates, see [WebView.postVisualStateCallback].
 *
 * Please note that all the conditions and recommendations applicable to
 * [WebView.postVisualStateCallback] also apply to this API.
 *
 * This callback is only called for main frame navigations.
 */
class PageCommitVisibleEvent(
    /**
     * The [WebView] for which the navigation occurred.
     */
    val view: WebView,
    /**
     * The URL corresponding to the page navigation that triggered this callback.
     */
    val url: String
)

/**
 * Report web resource loading error to the host application. These errors usually indicate
 * inability to connect to the server. Note that unlike the deprecated version of the callback,
 * the new version will be called for any resource (iframe, image, etc.), not just for the main
 * page. Thus, it is recommended to perform minimum required work in this callback.
 */
class ReceivedErrorEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * The originating request.
     */
    val request: WebResourceRequest,

    /**
     * Information about the error occurred.
     */
    val error: WebResourceError
)

/**
 * Notify the host application that an HTTP error has been received from the server while
 * loading a resource.  HTTP errors have status codes &gt;= 400.  This callback will be called
 * for any resource (iframe, image, etc.), not just for the main page. Thus, it is recommended
 * to perform minimum required work in this callback. Note that the content of the server
 * response may not be provided within the `errorResponse` parameter.
 */
class ReceivedHttpErrorEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * The originating request.
     */
    val request: WebResourceRequest,

    /**
     * Information about the error occurred.
     */
    val errorResponse: WebResourceResponse
)

/**
 * As the host application if the browser should resend data as the
 * requested page was a result of a POST. The default is to not resend the
 * data.
 */
class FormResubmissionEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * The message to send if the browser should not resend
     */
    val dontResend: Message,

    /**
     * The message to send if the browser should resend data
     */
    val resend: Message
) : SyncHandlerEvent()

/**
 * Notify the host application to update its visited links database.
 */
class UpdateVisitedHistoryEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * The url being visited.
     */
    val url: String,

    /**
     * `true` if this url is being reloaded.
     */
    val isReload: Boolean
)

/**
 * Notify the host application that an SSL error occurred while loading a
 * resource. The host application must call either [SslErrorHandler.cancel] or
 * [SslErrorHandler.proceed]. Note that the decision may be retained for use in
 * response to future SSL errors. The default behavior is to cancel the
 * load.
 *
 * This API is only called for recoverable SSL certificate errors. In the case of
 * non-recoverable errors (such as when the server fails the client), WebView will call
 * [ReceivedErrorEvent] with [WebViewClient.ERROR_FAILED_SSL_HANDSHAKE].
 *
 * Applications are advised not to prompt the user about SSL errors, as
 * the user is unlikely to be able to make an informed security decision
 * and WebView does not provide any UI for showing the details of the
 * error in a meaningful way.
 *
 * Application overrides of this method may display custom error pages or
 * silently log issues, but it is strongly recommended to always call
 * [SslErrorHandler.cancel] and never allow proceeding past errors.
 */
class ReceivedSslErrorEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * An [SslErrorHandler] that will handle the user's response.
     */
    val handler: SslErrorHandler,

    /**
     * The SSL error object.
     */
    val error: SslError
) : SyncHandlerEvent()

/**
 * Notify the host application to handle a SSL client certificate request. The host application
 * is responsible for showing the UI if desired and providing the keys. There are three ways to
 * respond: [ClientCertRequest.proceed], [ClientCertRequest.cancel], or [ClientCertRequest.ignore].
 * Webview stores the response in memory (for the life of the
 * application) if [ClientCertRequest.proceed] or [ClientCertRequest.cancel] is
 * called and does not call `onReceivedClientCertRequest()` again for the same host and
 * port pair. Webview does not store the response if [ClientCertRequest.ignore]
 * is called. Note that, multiple layers in chromium network stack might be
 * caching the responses, so the behavior for ignore is only a best case
 * effort.
 *
 * This method is called on the UI thread. During the callback, the
 * connection is suspended.
 *
 * For most use cases, the application program should implement the
 * [android.security.KeyChainAliasCallback] interface and pass it to
 * [android.security.KeyChain.choosePrivateKeyAlias] to start an
 * activity for the user to choose the proper alias. The keychain activity will
 * provide the alias through the callback method in the implemented interface. Next
 * the application should create an async task to call
 * [android.security.KeyChain.getPrivateKey] to receive the key.
 *
 * An example implementation of client certificates can be seen at
 * [AOSP Browser](https://android.googlesource.com/platform/packages/apps/Browser/+/android-5.1.1_r1/src/com/android/browser/Tab.java)
 *
 * The default behavior is to cancel, returning no client certificate.
 */
class ReceivedClientCertRequestEvent(
    /**
     * The WebView that is initiating the callback
     */
    val view: WebView,

    /**
     * An instance of a [ClientCertRequest]
     */
    val request: ClientCertRequest
) : SyncHandlerEvent()

/**
 * Notifies the host application that the WebView received an HTTP
 * authentication request. The host application can use the supplied
 * [HttpAuthHandler] to set the WebView's response to the request.
 * The default behavior is to cancel the request.
 */
class ReceivedHttpAuthRequestEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * The HttpAuthHandler used to set the WebView's response.
     */
    val handler: HttpAuthHandler,

    /**
     * The host requiring authentication.
     */
    val host: String,

    /**
     * The realm for which authentication is required.
     */
    val realm: String
) : SyncHandlerEvent()

/**
 * Notify the host application that a key was not handled by the WebView.
 * Except system keys, WebView always consumes the keys in the normal flow
 * or if [ShouldOverrideKeyEventEvent] returns `true`. This is called asynchronously
 * from where the key is dispatched. It gives the host application a chance
 * to handle the unhandled key events.
 */
class UnhandledKeyEventEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * The key event.
     */
    val event: KeyEvent
)

/**
 * Notify the host application that the scale applied to the WebView has changed.
 */
class ScaleChangedEvent(
    /**
     * The WebView that is initiating the callback.
     */
    val view: WebView,

    /**
     * The old scale factor
     */
    val oldScale: Float,

    /**
     * The new scale factor
     */
    val newScale: Float
)

/**
 * Notify the host application that a request to automatically log in the
 * user has been processed.
 */
class ReceivedLoginRequestEvent(
    /**
     * The WebView requesting the login.
     */
    val view: WebView,

    /**
     * The account realm used to look up accounts.
     */
    val realm: String,

    /**
     * An optional account. If not `null`, the account should be checked against accounts on the device. If it is a valid account, it should be used to log in the user.
     */
    val account: String?,

    /**
     * Authenticator specific arguments used to log in the user.
     */
    val args: String
)

/**
 * Notify host application that the given WebView's render process has exited.
 *
 * Multiple WebView instances may be associated with a single render process;
 * onRenderProcessGone will be called for each WebView that was affected.
 * The application's implementation of this callback should only attempt to
 * clean up the specific WebView given as a parameter, and should not assume
 * that other WebView instances are affected.
 *
 * The given WebView can't be used, and should be removed from the view hierarchy,
 * all references to it should be cleaned up, e.g any references in the Activity
 * or other classes saved using [android.view.View.findViewById] and similar calls, etc.
 *
 * To cause an render process crash for test purpose, the application can
 * call `loadUrl("chrome://crash")` on the WebView. Note that multiple WebView
 * instances may be affected if they share a render process, not just the
 * specific WebView which loaded chrome://crash.
 *
 * Call [SyncHandlerEvent.shouldHandle]  if the host application handled the situation
 * that process has exited, otherwise, application will crash if render process crashed,
 * or be killed if render process was killed by the system.
 */
class RenderProcessGoneEvent(
    /**
     * The WebView which needs to be cleaned up.
     */
    val view: WebView,

    /**
     * the reason why it exited.
     */
    val detail: RenderProcessGoneDetail
) : SyncHandlerEvent()

/**
 * Notify the host application that a loading URL has been flagged by Safe Browsing.
 *
 * The application must invoke the callback to indicate the preferred response. The default
 * behavior is to show an interstitial to the user, with the reporting checkbox visible.
 *
 * If the application needs to show its own custom interstitial UI, the callback can be invoked
 * asynchronously with [SafeBrowsingResponse.backToSafety] or [SafeBrowsingResponse.proceed],
 * depending on user response.
 */
class SafeBrowsingHitEvent(
    /**
     * The WebView that hit the malicious resource.
     */
    val view: WebView,

    /**
     * Object containing the details of the request.
     */
    val request: WebResourceRequest,

    /**
     * The reason the resource was caught by Safe Browsing, corresponding to a `SAFE_BROWSING_THREAT_*` value.
     */
    val threatType: Int,

    /**
     * Applications must invoke one of the callback methods.
     */
    val callback: SafeBrowsingResponse
)

open class RacehorseWebViewClient(private val c: RacehorseConnection) : WebViewClient() {

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return ShouldInterceptRequestEvent(view, request, null).let {
            c.post(it)
            it.response
        }
    }

    override fun shouldOverrideKeyEvent(view: WebView, event: KeyEvent): Boolean {
        return ShouldOverrideKeyEventEvent(view, event).let {
            c.post(it)
            it.isHandled
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        return ShouldOverrideUrlLoadingEvent(view, request).let {
            c.post(it)
            it.isHandled
        }
    }

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        c.post(PageStartedEvent(view, url, favicon))
    }

    override fun onPageFinished(view: WebView, url: String) {
        c.post(PageFinishedEvent(view, url))
    }

    override fun onLoadResource(view: WebView, url: String) {
        c.post(LoadResourceEvent(view, url))
    }

    override fun onPageCommitVisible(view: WebView, url: String) {
        c.post(PageCommitVisibleEvent(view, url))
    }

    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
        c.post(ReceivedErrorEvent(view, request, error))
    }

    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, errorResponse: WebResourceResponse) {
        c.post(ReceivedHttpErrorEvent(view, request, errorResponse))
    }

    override fun onFormResubmission(view: WebView, dontResend: Message, resend: Message) {
        FormResubmissionEvent(view, dontResend, resend).let {
            c.post(it)

            if (!it.isHandled) {
                super.onFormResubmission(view, dontResend, resend)
            }
        }
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
        c.post(UpdateVisitedHistoryEvent(view, url, isReload))
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        ReceivedSslErrorEvent(view, handler, error).let {
            c.post(it)

            if (!it.isHandled) {
                super.onReceivedSslError(view, handler, error)
            }
        }
    }

    override fun onReceivedClientCertRequest(view: WebView, request: ClientCertRequest) {
        ReceivedClientCertRequestEvent(view, request).let {
            c.post(it)

            if (!it.isHandled) {
                super.onReceivedClientCertRequest(view, request)
            }
        }
    }

    override fun onReceivedHttpAuthRequest(view: WebView, handler: HttpAuthHandler, host: String, realm: String) {
        ReceivedHttpAuthRequestEvent(view, handler, host, realm).let {
            c.post(it)

            if (!it.isHandled) {
                super.onReceivedHttpAuthRequest(view, handler, host, realm)
            }
        }
    }

    override fun onUnhandledKeyEvent(view: WebView, event: KeyEvent) {
        c.post(UnhandledKeyEventEvent(view, event))
    }

    override fun onScaleChanged(view: WebView, oldScale: Float, newScale: Float) {
        c.post(ScaleChangedEvent(view, oldScale, newScale))
    }

    override fun onReceivedLoginRequest(view: WebView, realm: String, account: String?, args: String) {
        c.post(ReceivedLoginRequestEvent(view, realm, account, args))
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        return RenderProcessGoneEvent(view, detail).let {
            c.post(it)
            it.isHandled
        }
    }

    override fun onSafeBrowsingHit(
        view: WebView,
        request: WebResourceRequest,
        threatType: Int,
        callback: SafeBrowsingResponse
    ) {
        c.post(SafeBrowsingHitEvent(view, request, threatType, callback))
    }
}
