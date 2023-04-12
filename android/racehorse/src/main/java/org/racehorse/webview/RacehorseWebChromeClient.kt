package org.racehorse.webview

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.*
import org.greenrobot.eventbus.EventBus
import org.racehorse.utils.HandlerEvent
import org.racehorse.utils.postForHandler
import org.racehorse.utils.postForSubscriber

class ProgressChangedEvent(val view: WebView, val progress: Int)

class ReceivedTitleEvent(val view: WebView, val title: String)

class ReceivedIconEvent(val view: WebView, val icon: Bitmap)

class ReceivedTouchIconUrlEvent(val view: WebView, val url: String, val precomposed: Boolean)

class ShowCustomViewEvent(val view: View, val callback: WebChromeClient.CustomViewCallback) : HandlerEvent()

class HideCustomViewEvent

class CreateWindowEvent(
    val view: WebView,
    val isDialog: Boolean,
    val isUserGesture: Boolean,
    val resultMessage: Message
) : HandlerEvent()

class RequestFocusEvent(val view: WebView)

class CloseWindowEvent(val window: WebView)

class JsAlertEvent(
    val view: WebView,
    val url: String,
    val message: String,
    val result: JsResult
) : HandlerEvent()

class JsConfirmEvent(
    val view: WebView,
    val url: String,
    val message: String,
    val result: JsResult
) : HandlerEvent()

class JsPromptEvent(
    val view: WebView,
    val url: String,
    val message: String,
    val defaultValue: String,
    val result: JsPromptResult
) : HandlerEvent()

class JsBeforeUnloadEvent(
    val view: WebView,
    val url: String,
    val message: String,
    val result: JsResult
) : HandlerEvent()

class GeolocationPermissionsShowPromptEvent(
    val origin: String,
    val callback: GeolocationPermissions.Callback
) : HandlerEvent()

class GeolocationPermissionsHidePromptEvent

class PermissionRequestEvent(val request: PermissionRequest) : HandlerEvent()

class PermissionRequestCanceledEvent(val request: PermissionRequest)

class ConsoleMessageEvent(val consoleMessage: ConsoleMessage) : HandlerEvent()

class ShowFileChooserEvent(
    val view: WebView,
    val filePathCallback: ValueCallback<Array<Uri>>,
    val fileChooserParams: WebChromeClient.FileChooserParams
) : HandlerEvent()

open class RacehorseWebChromeClient(val eventBus: EventBus = EventBus.getDefault()) : WebChromeClient() {

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
        return eventBus.postForHandler { CreateWindowEvent(view, isDialog, isUserGesture, resultMessage) }
    }

    override fun onRequestFocus(view: WebView) {
        eventBus.postForSubscriber { RequestFocusEvent(view) }
    }

    override fun onCloseWindow(window: WebView) {
        eventBus.postForSubscriber { CloseWindowEvent(window) }
    }

    override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return eventBus.postForHandler { JsAlertEvent(view, url, message, result) }
    }

    override fun onJsConfirm(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return eventBus.postForHandler { JsConfirmEvent(view, url, message, result) }
    }

    override fun onJsPrompt(
        view: WebView,
        url: String,
        message: String,
        defaultValue: String,
        result: JsPromptResult
    ): Boolean {
        return eventBus.postForHandler { JsPromptEvent(view, url, message, defaultValue, result) }
    }

    override fun onJsBeforeUnload(view: WebView, url: String, message: String, result: JsResult): Boolean {
        return eventBus.postForHandler { JsBeforeUnloadEvent(view, url, message, result) }
    }

    override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
        eventBus.postForSubscriber { GeolocationPermissionsShowPromptEvent(origin, callback) }
    }

    override fun onGeolocationPermissionsHidePrompt() {
        eventBus.postForSubscriber { GeolocationPermissionsHidePromptEvent() }
    }

    override fun onPermissionRequest(request: PermissionRequest) {
        if (!eventBus.postForHandler { PermissionRequestEvent(request) }) {
            request.deny()
        }
    }

    override fun onPermissionRequestCanceled(request: PermissionRequest) {
        eventBus.postForSubscriber { PermissionRequestCanceledEvent(request) }
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        return eventBus.postForHandler { ConsoleMessageEvent(consoleMessage) }
    }

    override fun onShowFileChooser(
        view: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        return eventBus.postForHandler { ShowFileChooserEvent(view, filePathCallback, fileChooserParams) }
    }
}
