package org.racehorse

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.widget.FrameLayout
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.Serializable
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.max

class KeyboardStatus(val height: Int) : Serializable {
    val isVisible = height != 0
}

class GetKeyboardStatusEvent : RequestEvent() {
    class ResultEvent(val status: KeyboardStatus) : ResponseEvent()
}

/**
 * Notifies the web app that the keyboard status has changed.
 */
class KeyboardStatusChangedEvent(val status: KeyboardStatus) : NoticeEvent

/**
 * Monitors keyboard visibility.
 *
 * @param activity The activity to which the keyboard observer is attached.
 * @param eventBus The event bus to which events are posted.
 */
open class KeyboardPlugin(private val activity: Activity, private val eventBus: EventBus = EventBus.getDefault()) {

    private val keyboardObserver = KeyboardObserver(activity) { keyboardHeight ->
        eventBus.post(KeyboardStatusChangedEvent(KeyboardStatus(keyboardHeight)))
    }

    @Subscribe
    open fun onGetKeyboardStatus(event: GetKeyboardStatusEvent) {
        event.respond(GetKeyboardStatusEvent.ResultEvent(KeyboardStatus(keyboardObserver.keyboardHeight)))
    }
}

/**
 * Observes the keyboard height.
 *
 * @param activity The activity for which the keyboard is observed.
 * @param listener The listener that receives keyboard updates.
 */
class KeyboardObserver(activity: Activity, private val listener: (keyboardHeight: Int) -> Unit) {

    /**
     * The height that the keyboard currently occupies on the screen.
     */
    val keyboardHeight get() = lastKeyboardHeight.get()

    private var lastKeyboardHeight = AtomicInteger()

    private val rootView = activity.window.decorView.findViewById<FrameLayout>(android.R.id.content).rootView

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener {
        val frameBottom = Rect().apply { rootView.getWindowVisibleDisplayFrame(this) }.bottom

        val insetBottom = if (Build.VERSION.SDK_INT >= 30) {
            rootView.rootWindowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()).bottom
        } else {
            @Suppress("DEPRECATION")
            rootView.rootWindowInsets.stableInsetBottom
        }

        val height = max(rootView.rootView.height - frameBottom - insetBottom, 0)

        if (lastKeyboardHeight.getAndSet(height) != height) {
            listener(height)
        }
    }

    init {
        rootView.viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
    }

    /**
     * Unregisters listener so it won't receive keyboard updates anymore.
     */
    fun unregister() {
        rootView.viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
    }
}
