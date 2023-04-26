package org.racehorse

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.view.ViewTreeObserver
import android.view.WindowInsets
import android.widget.FrameLayout
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.atomic.AtomicInteger

class IsKeyboardVisibleEvent : RequestEvent() {
    class ResultEvent(val isKeyboardVisible: Boolean) : ResponseEvent()
}

/**
 * Notifies the web app that the keyboard visibility has changed.
 */
class KeyboardVisibilityChangedEvent(val isKeyboardVisible: Boolean) : NoticeEvent

/**
 * Monitors keyboard visibility.
 *
 * @param activity The activity to which insets listener is attached.
 * @param eventBus The event bus to which events are posted.
 */
open class KeyboardPlugin(activity: Activity, private val eventBus: EventBus = EventBus.getDefault()) {

    private val keyboardObserver = KeyboardObserver(activity) { keyboardHeight ->
        eventBus.post(KeyboardVisibilityChangedEvent(keyboardHeight > 0))
    }

    @Subscribe
    open fun onIsKeyboardVisible(event: IsKeyboardVisibleEvent) {
        eventBus.post(IsKeyboardVisibleEvent.ResultEvent(keyboardObserver.keyboardHeight > 0))
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

        val height = rootView.rootView.height - frameBottom - insetBottom

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
