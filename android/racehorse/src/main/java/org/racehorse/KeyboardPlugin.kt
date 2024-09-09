package org.racehorse

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.Serializable

/**
 * The status of the software keyboard.
 */
class KeyboardStatus(val height: Float) : Serializable {
    val isVisible = height > 0
}

/**
 * An animation that can be marshalled to a web view.
 *
 * @param startValue A value from which an animation starts.
 * @param endValue A value at which an animation ends.
 * @param duration An animation duration in milliseconds.
 * @param easingValues An easing curve described as an array of at least two ordinate values (y ∈ [0, 1]) that correspond
 * to an equidistant abscissa values (y).
 * @param startTimestamp A timestamp when an animation has started.
 */
class Animation(
    val startValue: Float,
    val endValue: Float,
    val duration: Int,
    val easingValues: FloatArray,
    val startTimestamp: Long = System.currentTimeMillis()
) : Serializable

/**
 * Get the current status of the software keyboard.
 */
class GetKeyboardStatusEvent : RequestEvent() {
    class ResultEvent(val status: KeyboardStatus) : ResponseEvent()
}

/**
 * Notifies the web app that the keyboard animation has started.
 */
class BeforeKeyboardStatusChangedEvent(val status: KeyboardStatus, val animation: Animation) : NoticeEvent

/**
 * Notifies the web app that the keyboard is fully shown or hidden. Event is published after an animation has finished.
 *
 * @param status The current status of the software keyboard.
 */
class KeyboardStatusChangedEvent(val status: KeyboardStatus) : NoticeEvent

/**
 * Shows the software keyboard.
 */
class ShowKeyboardEvent : WebEvent

/**
 * Hides the software keyboard.
 */
class HideKeyboardEvent : WebEvent

/**
 * Monitors keyboard visibility.
 *
 * @param activity The activity to which the keyboard observer is attached.
 * @param eventBus The event bus to which events are posted.
 */
open class KeyboardPlugin(private val activity: Activity, private val eventBus: EventBus = EventBus.getDefault()) {

    private val inputMethodManager by lazy { activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    fun enable() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // Support older Android versions
            activity.window.decorView.viewTreeObserver.addOnGlobalLayoutListener(
                KeyboardGlobalLayoutListener(activity, eventBus)
            )
            return
        }

        ViewCompat.setWindowInsetsAnimationCallback(
            activity.window.decorView,
            KeyboardInsetsAnimationCallback(activity, eventBus)
        )
    }

    @Subscribe
    open fun onGetKeyboardStatus(event: GetKeyboardStatusEvent) {
        event.respond(GetKeyboardStatusEvent.ResultEvent(KeyboardStatus(0f)))
    }

    @Subscribe
    open fun onShowKeyboard(event: ShowKeyboardEvent) {
        inputMethodManager.showSoftInput(activity.currentFocus, 0)
    }

    @Subscribe
    open fun onHideKeyboard(event: HideKeyboardEvent) {
        inputMethodManager.hideSoftInputFromWindow(
            activity.currentFocus?.windowToken,
            InputMethodManager.HIDE_NOT_ALWAYS
        )
    }
}

private class KeyboardInsetsAnimationCallback(private val activity: Activity, private val eventBus: EventBus) :
    WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {

    companion object {
        private const val FRAMES_PER_SECOND = 60

        /**
         * The maximum number of frames serialized for an animation easing.
         */
        private const val MAX_FRAME_COUNT = 200
    }

    private var startHeight = 0f
    private var endHeight = 0f

    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        if (animation.typeMask and Type.ime() != 0) {
            startHeight = getKeyboardHeight()
        }
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ): WindowInsetsAnimationCompat.BoundsCompat {
        if (animation.typeMask and Type.ime() != 0) {
            endHeight = getKeyboardHeight()
            eventBus.post(BeforeKeyboardStatusChangedEvent(KeyboardStatus(endHeight), getKeyboardAnimation(animation)))
        }
        return bounds
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        if (animation.typeMask and Type.ime() != 0) {
            eventBus.post(KeyboardStatusChangedEvent(KeyboardStatus(endHeight)))
        }
    }

    override fun onProgress(
        windowInsets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        return windowInsets
    }

    private fun getKeyboardHeight(): Float {
        val windowInsets = ViewCompat.getRootWindowInsets(activity.window.decorView) ?: return 0f

        return windowInsets.getInsets(Type.ime()).bottom / activity.resources.displayMetrics.density
    }

    private fun getKeyboardAnimation(animation: WindowInsetsAnimationCompat): Animation {
        val durationScale =
            Settings.Global.getFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)

        val duration = (animation.durationMillis * durationScale).toInt()

        val easingValues = animation.interpolator
            ?.let {
                val frameCount = (FRAMES_PER_SECOND * duration / 1000).coerceIn(2, MAX_FRAME_COUNT)

                FloatArray(frameCount).apply {
                    forEachIndexed { index, _ ->
                        this[index] = (it.getInterpolation(index / (frameCount - 1f)) * 1000).toInt() / 1000f
                    }
                }
            }
            ?: floatArrayOf(0f, 1f)

        return Animation(startHeight, endHeight, duration, easingValues)
    }
}

private class KeyboardGlobalLayoutListener(private val activity: Activity, private val eventBus: EventBus) :
    ViewTreeObserver.OnGlobalLayoutListener {

    private var prevHeight = 0f

    override fun onGlobalLayout() {
        val height = getKeyboardHeight()

        // Safe to compare because of stable arithmetic
        if (prevHeight == height) {
            return
        }

        eventBus.post(
            BeforeKeyboardStatusChangedEvent(
                KeyboardStatus(height),
                Animation(prevHeight, height, 0, floatArrayOf(0f, 1f))
            )
        )

        eventBus.post(KeyboardStatusChangedEvent(KeyboardStatus(height)))

        prevHeight = height
    }

    private fun getKeyboardHeight(): Float {
        val windowInsets = ViewCompat.getRootWindowInsets(activity.window.decorView) ?: return 0f

        return windowInsets.getInsets(Type.ime()).bottom / activity.resources.displayMetrics.density
    }
}
