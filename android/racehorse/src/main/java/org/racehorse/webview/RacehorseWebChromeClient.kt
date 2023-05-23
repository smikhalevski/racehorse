package org.racehorse.webview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import org.greenrobot.eventbus.EventBus
import org.racehorse.utils.SyncHandlerEvent
import org.racehorse.utils.postForSubscriber
import org.racehorse.utils.postForSyncHandler

class ProgressChangedEvent(val view: WebView, val progress: Int)

class ReceivedTitleEvent(val view: WebView, val title: String)

class ReceivedIconEvent(val view: WebView, val icon: Bitmap)

class ReceivedTouchIconUrlEvent(val view: WebView, val url: String, val precomposed: Boolean)

class ShowCustomViewEvent(val view: View, val callback: WebChromeClient.CustomViewCallback)

class HideCustomViewEvent

class CreateWindowEvent(
    val view: WebView,
    val isDialog: Boolean,
    val isUserGesture: Boolean,
    val resultMessage: Message
) : SyncHandlerEvent()

class RequestFocusEvent(val view: WebView)

class CloseWindowEvent(val window: WebView)

class JsAlertEvent(
    val view: WebView,
    val url: String,
    val message: String,
    val result: JsResult
) : SyncHandlerEvent()

class JsConfirmEvent(
    val view: WebView,
    val url: String,
    val message: String,
    val result: JsResult
) : SyncHandlerEvent()

class JsPromptEvent(
    val view: WebView,
    val url: String,
    val message: String,
    val defaultValue: String,
    val result: JsPromptResult
) : SyncHandlerEvent()

class JsBeforeUnloadEvent(
    val view: WebView,
    val url: String,
    val message: String,
    val result: JsResult
) : SyncHandlerEvent()

class GeolocationPermissionsShowPromptEvent(
    val origin: String,
    val callback: GeolocationPermissions.Callback
)

class GeolocationPermissionsHidePromptEvent

class PermissionRequestEvent(val request: PermissionRequest) : SyncHandlerEvent()

class PermissionRequestCanceledEvent(val request: PermissionRequest)

class ConsoleMessageEvent(val consoleMessage: ConsoleMessage) : SyncHandlerEvent()

class ShowFileChooserEvent(
    val view: WebView,
    val filePathCallback: ValueCallback<Array<Uri>>,
    val fileChooserParams: WebChromeClient.FileChooserParams
) : SyncHandlerEvent()

open class RacehorseWebChromeClient(val eventBus: EventBus = EventBus.getDefault()) : WebChromeClient() {

    private val transparentPoster = Bitmap.createBitmap(1, 1, Bitmap.Config.ALPHA_8).apply {
        Canvas(this).drawARGB(255, 0, 0, 0)
    }

    // Replace ugly default poster with a transparent image
    override fun getDefaultVideoPoster(): Bitmap? = transparentPoster

    override fun onProgressChanged(view: WebView, progress: Int) {
        eventBus.postForSubscriber { ProgressChangedEvent(view, progress) }
    }

    override fun onReceivedTitle(view: WebView, title: String) {
        eventBus.postForSubscriber { ReceivedTitleEvent(view, title) }
    }

    override fun onReceivedIcon(view: WebView, icon: Bitmap) {
        eventBus.postForSubscriber { ReceivedIconEvent(view, icon) }
    }

    override fun onReceivedTouchIconUrl(view: WebView, url: String, precomposed: Boolean) {
        eventBus.postForSubscriber { ReceivedTouchIconUrlEvent(view, url, precomposed) }
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        eventBus.postForSubscriber { ShowCustomViewEvent(view, callback) }
    }

    override fun onHideCustomView() {
        eventBus.postForSubscriber { HideCustomViewEvent() }
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMessage: Message
    ): Boolean {
        return eventBus.postForSyncHandler { CreateWindowEvent(view, isDialog, isUserGesture, resultMessage) }
    }

    override fun onRequestFocus(view: WebView) {
        eventBus.postForSubscriber { RequestFocusEvent(view) }
    }

    override fun onCloseWindow(window: WebView) {
        eventBus.postForSubscriber { CloseWindowEvent(window) }
    }

    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return eventBus.postForSyncHandler { JsAlertEvent(view, url, message, result) }
    }

    override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return eventBus.postForSyncHandler { JsConfirmEvent(view, url, message, result) }
    }

    override fun onJsPrompt(
        view: WebView,
        url: String,
        message: String,
        defaultValue: String,
        result: JsPromptResult
    ): Boolean {
        return eventBus.postForSyncHandler { JsPromptEvent(view, url, message, defaultValue, result) }
    }

    override fun onJsBeforeUnload(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return eventBus.postForSyncHandler { JsBeforeUnloadEvent(view, url, message, result) }
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        eventBus.postForSubscriber { GeolocationPermissionsShowPromptEvent(origin, callback) }
    }

    override fun onGeolocationPermissionsHidePrompt() {
        eventBus.postForSubscriber { GeolocationPermissionsHidePromptEvent() }
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        if (!eventBus.postForSyncHandler { PermissionRequestEvent(request) }) {
            super.onPermissionRequest(request)
        }
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        eventBus.postForSubscriber { PermissionRequestCanceledEvent(request) }
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        return eventBus.postForSyncHandler { ConsoleMessageEvent(consoleMessage) }
    }

    override fun onShowFileChooser(
        view: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        return eventBus.postForSyncHandler { ShowFileChooserEvent(view, filePathCallback, fileChooserParams) }
    }
}
