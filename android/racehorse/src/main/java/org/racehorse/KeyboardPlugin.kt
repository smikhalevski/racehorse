package org.racehorse

import androidx.activity.ComponentActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.util.concurrent.atomic.AtomicBoolean

class IsKeyboardVisibleRequestEvent : RequestEvent()

class IsKeyboardVisibleResponseEvent(val isKeyboardVisible: Boolean) : ResponseEvent()

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
open class KeyboardPlugin(activity: ComponentActivity, private val eventBus: EventBus = EventBus.getDefault()) {

    private val lastVisible = AtomicBoolean()

    init {
        var initialHeight = -1

        activity.window.decorView.setOnApplyWindowInsetsListener { _, windowInsets ->
            val height = toWindowInsetsCompat(windowInsets).getInsets(WindowInsetsCompat.Type.ime()).bottom

            if (initialHeight == -1) {
                initialHeight = height
            }

            val visible = height - initialHeight > 0

            if (lastVisible.compareAndSet(!visible, visible)) {
                eventBus.post(KeyboardVisibilityChangedEvent(visible))
            }
            windowInsets
        }
    }

    @Subscribe
    open fun onIsKeyboardVisible(event: IsKeyboardVisibleRequestEvent) {
        eventBus.post(IsKeyboardVisibleResponseEvent(lastVisible.get()))
    }
}
