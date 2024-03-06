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

/**
 * Tell the host application the current progress of loading a page.
 */
class ProgressChangedEvent(
    /**
     * The WebView that initiated the callback.
     */
    val view: WebView,

    /**
     * Current page loading progress, represented by an integer between 0 and 100.
     */
    val progress: Int
)

/**
 * Notify the host application of a change in the document title.
 */
class ReceivedTitleEvent(
    /**
     * The WebView that initiated the callback.
     */
    val view: WebView,

    /**
     * A String containing the new title of the document.
     */
    val title: String
)

/**
 * Notify the host application of a new favicon for the current page.
 */
class ReceivedIconEvent(
    /**
     * The WebView that initiated the callback.
     */
    val view: WebView,

    /**
     * A Bitmap containing the favicon for the current page.
     */
    val icon: Bitmap
)

/**
 * Notify the host application of the url for an apple-touch-icon.
 */
class ReceivedTouchIconUrlEvent(
    /**
     * The WebView that initiated the callback.
     */
    val view: WebView,

    /**
     * The icon url.
     */
    val url: String,

    /**
     * `true` if the url is for a precomposed touch icon.
     */
    val isPrecomposed: Boolean
)

/**
 * Notify the host application that the current page has entered full screen mode. After this
 * call, web content will no longer be rendered in the WebView, but will instead be rendered
 * in `view`. The host application should add this View to a Window which is configured
 * with [android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN] flag in order to
 * actually display this web content full screen.
 *
 * The application may explicitly exit fullscreen mode by invoking `callback` (ex. when
 * the user presses the back button). However, this is generally not necessary as the web page
 * will often show its own UI to close out of fullscreen. Regardless of how the WebView exits
 * fullscreen mode, WebView will invoke [HideCustomViewEvent], signaling for the
 * application to remove the custom View.
 *
 * If this method is not overridden, WebView will report to the web page it does not support
 * fullscreen mode and will not honor the web page's request to run in fullscreen mode.
 *
 * **Note:** if overriding this method, the application must also override [HideCustomViewEvent].
 */
class ShowCustomViewEvent(
    /**
     * Is the View object to be shown.
     */

    val view: View,

    /**
     * Invoke this callback to request the page to exit full screen mode.
     */
    val callback: WebChromeClient.CustomViewCallback
)

/**
 * Notify the host application that the current page has exited full screen mode. The host
 * application must hide the custom View (the View which was previously passed to [ShowCustomViewEvent]).
 * After this call, web content will render in the original WebView again.
 *
 * **Note:** if overriding this method, the application must also override [ShowCustomViewEvent].
 */
class HideCustomViewEvent

/**
 * Request the host application to create a new window. If the host
 * application chooses to honor this request, it should return `true` from
 * this method, create a new WebView to host the window, insert it into the
 * View system and send the supplied resultMsg message to its target with
 * the new WebView as an argument. If the host application chooses not to
 * honor the request, it should return `false` from this method. The default
 * implementation of this method does nothing and hence returns `false`.
 *
 * Applications should typically not allow windows to be created when the
 * `isUserGesture` flag is false, as this may be an unwanted popup.
 *
 * Applications should be careful how they display the new window: don't simply
 * overlay it over the existing WebView as this may mislead the user about which
 * site they are viewing. If your application displays the URL of the main page,
 * make sure to also display the URL of the new window in a similar fashion. If
 * your application does not display URLs, consider disallowing the creation of
 * new windows entirely.
 * **Note:** There is no trustworthy way to tell which page
 * requested the new window: the request might originate from a third-party iframe
 * inside the WebView.
 *
 * Use [SyncHandlerEvent.shouldHandle] if the host application will create a new window,
 * in which case resultMsg should be sent to its target. Otherwise, this method should
 * return `false`. Returning `false` from this method but also sending resultMsg will
 * result in undefined behavior.
 */
class CreateWindowEvent(

    /**
     * The WebView from which the request for a new window originated.
     */
    val view: WebView,

    /**
     * `true` if the new window should be a dialog, rather than a full-size window.
     */
    val isDialog: Boolean,

    /**
     * `true` if the request was initiated by a user gesture, such as the user clicking a link.
     */
    val isUserGesture: Boolean,

    /**
     * The message to send when once a new WebView has been created. resultMsg.obj is a [WebView.WebViewTransport]
     * object. This should be used to transport the new WebView, by calling [WebView.WebViewTransport.setWebView].
     */
    val resultMessage: Message
) : SyncHandlerEvent()

/**
 * Request display and focus for this WebView. This may happen due to
 * another WebView opening a link in this WebView and requesting that this
 * WebView be displayed.
 */
class RequestFocusEvent(
    /**
     * The WebView that needs to be focused.
     */
    val view: WebView
)

/**
 * Notify the host application to close the given WebView and remove it
 * from the view system if necessary. At this point, WebCore has stopped
 * any loading in this window and has removed any cross-scripting ability
 * in javascript.
 *
 * As with [CreateWindowEvent], the application should ensure that any
 * URL or security indicator displayed is updated so that the user can tell
 * that the page they were interacting with has been closed.
 */
class CloseWindowEvent(
    /**
     * The WebView that needs to be closed.
     */
    val window: WebView
)

/**
 * Notify the host application that the web page wants to display a
 * JavaScript `alert()` dialog.
 * The default behavior if this method returns `false` or is not
 * overridden is to show a dialog containing the alert message and suspend
 * JavaScript execution until the dialog is dismissed.
 * To show a custom dialog, the app should return `true` from this
 * method, in which case the default dialog will not be shown and JavaScript
 * execution will be suspended. The app should call
 * `JsResult.confirm()` when the custom dialog is dismissed such that
 * JavaScript execution can be resumed.
 * To suppress the dialog and allow JavaScript execution to
 * continue, call `JsResult.confirm()` immediately and then return
 * `true`.
 * Note that if the [WebChromeClient] is set to be `null`,
 * or if [WebChromeClient] is not set at all, the default dialog will
 * be suppressed and Javascript execution will continue immediately.
 * Note that the default dialog does not inherit the [android.view.Display.FLAG_SECURE]
 * flag from the parent window.
 *
 * Call [SyncHandlerEvent.shouldHandle] if the request is handled or ignored, otherwise
 * WebView needs to show the default dialog.
 */
class JsAlertEvent(
    /**
     * The WebView that initiated the callback.
     */
    val view: WebView,

    /**
     * The url of the page requesting the dialog.
     */
    val url: String,

    /**
     * Message to be displayed in the window.
     */
    val message: String,

    /**
     * A JsResult to confirm that the user closed the window.
     */
    val result: JsResult
) : SyncHandlerEvent()

/**
 * Notify the host application that the web page wants to display a
 * JavaScript `confirm()` dialog.
 * The default behavior if this method returns `false` or is not
 * overridden is to show a dialog containing the message and suspend
 * JavaScript execution until the dialog is dismissed. The default dialog
 * will return `true` to the JavaScript `confirm()` code when
 * the user presses the 'confirm' button, and will return `false` to
 * the JavaScript code when the user presses the 'cancel' button or
 * dismisses the dialog.
 * To show a custom dialog, the app should return `true` from this
 * method, in which case the default dialog will not be shown and JavaScript
 * execution will be suspended. The app should call
 * `JsResult.confirm()` or `JsResult.cancel()` when the custom
 * dialog is dismissed.
 * To suppress the dialog and allow JavaScript execution to continue,
 * call `JsResult.confirm()` or `JsResult.cancel()` immediately
 * and then return `true`.
 * Note that if the [WebChromeClient] is set to be `null`,
 * or if [WebChromeClient] is not set at all, the default dialog will
 * be suppressed and the default value of `false` will be returned to
 * the JavaScript code immediately.
 * Note that the default dialog does not inherit the [android.view.Display.FLAG_SECURE]
 * flag from the parent window.
 *
 * Call [SyncHandlerEvent.shouldHandle] if the request is handled or ignored, otherwise
 * WebView needs to show the default dialog.
 */
class JsConfirmEvent(
    /**
     * The WebView that initiated the callback.
     */
    val view: WebView,

    /**
     * The url of the page requesting the dialog.
     */
    val url: String,

    /**
     * Message to be displayed in the window.
     */
    val message: String,

    /**
     * A JsResult used to send the user's response to javascript.
     */
    val result: JsResult
) : SyncHandlerEvent()

/**
 * Notify the host application that the web page wants to display a
 * JavaScript `prompt()` dialog.
 * The default behavior if this method returns `false` or is not
 * overridden is to show a dialog containing the message and suspend
 * JavaScript execution until the dialog is dismissed. Once the dialog is
 * dismissed, JavaScript `prompt()` will return the string that the
 * user typed in, or null if the user presses the 'cancel' button.
 * To show a custom dialog, the app should return `true` from this
 * method, in which case the default dialog will not be shown and JavaScript
 * execution will be suspended. The app should call
 * `JsPromptResult.confirm(result)` when the custom dialog is
 * dismissed.
 * To suppress the dialog and allow JavaScript execution to continue,
 * call `JsPromptResult.confirm(result)` immediately and then
 * return `true`.
 * Note that if the [WebChromeClient] is set to be `null`,
 * or if [WebChromeClient] is not set at all, the default dialog will
 * be suppressed and `null` will be returned to the JavaScript code
 * immediately.
 * Note that the default dialog does not inherit the [android.view.Display.FLAG_SECURE]
 * flag from the parent window.
 *
 * Call [SyncHandlerEvent.shouldHandle] if the request is handled or ignored, otherwise
 * WebView needs to show the default dialog.
 */
class JsPromptEvent(
    /**
     * The WebView that initiated the callback.
     */
    val view: WebView,

    /**
     * The url of the page requesting the dialog.
     */
    val url: String,

    /**
     * Message to be displayed in the window.
     */
    val message: String,

    /**
     * The default value displayed in the prompt dialog.
     */
    val defaultValue: String,

    /**
     * A JsPromptResult used to send the user's response to javascript.
     */
    val result: JsPromptResult
) : SyncHandlerEvent()

/**
 * Notify the host application that the web page wants to confirm navigation
 * from JavaScript `onbeforeunload`.
 * The default behavior if this method returns `false` or is not
 * overridden is to show a dialog containing the message and suspend
 * JavaScript execution until the dialog is dismissed. The default dialog
 * will continue the navigation if the user confirms the navigation, and
 * will stop the navigation if the user wants to stay on the current page.
 * To show a custom dialog, the app should return `true` from this
 * method, in which case the default dialog will not be shown and JavaScript
 * execution will be suspended. When the custom dialog is dismissed, the
 * app should call `JsResult.confirm()` to continue the navigation or,
 * `JsResult.cancel()` to stay on the current page.
 * To suppress the dialog and allow JavaScript execution to continue,
 * call `JsResult.confirm()` or `JsResult.cancel()` immediately
 * and then return `true`.
 * Note that if the [WebChromeClient] is set to be `null`,
 * or if [WebChromeClient] is not set at all, the default dialog will
 * be suppressed and the navigation will be resumed immediately.
 * Note that the default dialog does not inherit the [android.view.Display.FLAG_SECURE]
 * flag from the parent window.
 *
 * Call [SyncHandlerEvent.shouldHandle] if the request is handled or ignored, otherwise
 * WebView needs to show the default dialog.
 */
class JsBeforeUnloadEvent(
    /**
     * The WebView that initiated the callback.
     */
    val view: WebView,

    /**
     * The url of the page requesting the dialog.
     */
    val url: String,

    /**
     * Message to be displayed in the window.
     */
    val message: String,

    /**
     * A JsResult used to send the user's response to javascript.
     */
    val result: JsResult

) : SyncHandlerEvent()

/**
 * Notify the host application that web content from the specified origin
 * is attempting to use the Geolocation API, but no permission state is
 * currently set for that origin. The host application should invoke the
 * specified callback with the desired permission state. See
 * [GeolocationPermissions] for details.
 *
 * Note that for applications targeting Android N and later SDKs
 * (API level > [android.os.Build.VERSION_CODES.M])
 * this method is only called for requests originating from secure
 * origins such as https. On non-secure origins geolocation requests
 * are automatically denied.
 */
class GeolocationPermissionsShowPromptEvent(
    /**
     * The origin of the web content attempting to use the Geolocation API.
     */
    val origin: String,

    /**
     * The callback to use to set the permission state for the origin.
     */
    val callback: GeolocationPermissions.Callback
)

/**
 * Notify the host application that a request for Geolocation permissions,
 * made with a previous call to
 * [GeolocationPermissionsShowPromptEvent]
 * has been canceled. Any related UI should therefore be hidden.
 */
class GeolocationPermissionsHidePromptEvent

/**
 * Notify the host application that web content is requesting permission to
 * access the specified resources and the permission currently isn't granted
 * or denied. The host application must invoke [PermissionRequest.grant]
 * or [PermissionRequest.deny].
 *
 * If this method isn't overridden, the permission is denied.
 */
class PermissionRequestEvent(
    /**
     * The PermissionRequest from current web content.
     */
    val request: PermissionRequest
) : SyncHandlerEvent()

/**
 * Notify the host application that the given permission request
 * has been canceled. Any related UI should therefore be hidden.
 */
class PermissionRequestCanceledEvent(
    /**
     * The PermissionRequest that needs be canceled.
     */
    val request: PermissionRequest
)

/**
 * Report a JavaScript console message to the host application. The ChromeClient
 * should override this to process the log message as they see fit.
 */
class ConsoleMessageEvent(
    /**
     * Object containing details of the console message.
     */
    val consoleMessage: ConsoleMessage
) : SyncHandlerEvent()

/**
 * Tell the client to show a file chooser.
 *
 * This is called to handle HTML forms with 'file' input type, in response to the
 * user pressing the "Select File" button.
 * To cancel the request, call `filePathCallback.onReceiveValue(null)` and
 * return `true`.
 */
class ShowFileChooserEvent(
    /**
     * The WebView instance that is initiating the request.
     */
    val view: WebView,

    /**
     * Invoke this callback to supply the list of paths to files to upload, or `null` to cancel.
     * Must only be called if the [ShowFileChooserEvent] implementation returns `true`.
     */
    val filePathCallback: ValueCallback<Array<Uri>>,

    /**
     * Describes the mode of file chooser to be opened, and options to be used with it.
     */
    val fileChooserParams: WebChromeClient.FileChooserParams
) : SyncHandlerEvent()

/**
 * Posts various events triggered by the [WebView].
 */
open class RacehorseWebChromeClient(private val eventBus: EventBus = EventBus.getDefault()) : WebChromeClient() {

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

    override fun onReceivedTouchIconUrl(view: WebView, url: String, isPrecomposed: Boolean) {
        eventBus.postForSubscriber { ReceivedTouchIconUrlEvent(view, url, isPrecomposed) }
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
