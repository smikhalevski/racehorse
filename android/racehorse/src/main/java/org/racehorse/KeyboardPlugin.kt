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
import kotlinx.serialization.Serializable
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe

@Deprecated("Use GetKeyboardHeightEvent")
@Serializable
class KeyboardStatus(val height: Float, val isVisible: Boolean = height > 0)

@Deprecated("Use KeyboardToggledEvent")
@Serializable
class KeyboardStatusChangedEvent(val status: KeyboardStatus) : NoticeEvent

@Deprecated("Use GetKeyboardHeightEvent")
@Serializable
class GetKeyboardStatusEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val status: KeyboardStatus) : ResponseEvent()
}

/**
 * An animation that can be marshalled to a web view.
 *
 * @param startValue A value from which an animation starts.
 * @param endValue A value at which an animation ends.
 * @param duration An animation duration in milliseconds.
 * @param easing An easing curve described by an array of at least two ordinate values (y ∈ [0, 1]) that correspond to
 * an equidistant abscissa values (x).
 * @param startTime A timestamp when an animation has started.
 */
@Serializable
class TweenAnimation(
    val startValue: Float,
    val endValue: Float,
    val duration: Int,
    val easing: FloatArray,
    val startTime: Long = System.currentTimeMillis()
)

/**
 * Get the current keyboard height.
 */
@Serializable
class GetKeyboardHeightEvent : RequestEvent() {

    @Serializable
    class ResultEvent(val height: Float) : ResponseEvent()
}

/**
 * Shows the software keyboard.
 */
@Serializable
class ShowKeyboardEvent : WebEvent

/**
 * Hides the software keyboard.
 */
@Serializable
class HideKeyboardEvent : WebEvent

/**
 * Notifies the web app that the keyboard animation has started.
 */
@Serializable
class KeyboardAnimationStartedEvent(val animation: TweenAnimation) : NoticeEvent

/**
 * Monitors keyboard visibility.
 *
 * @param activity The activity to which the keyboard observer is attached.
 * @param eventBus The event bus to which events are posted.
 */
open class KeyboardPlugin(private val activity: Activity, private val eventBus: EventBus = EventBus.getDefault()) {

    private val inputMethodManager by lazy { activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    fun enable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ViewCompat.setWindowInsetsAnimationCallback(
                activity.window.decorView,
                KeyboardInsetsAnimationCallback(activity, eventBus)
            )
        } else {
            // Legacy Android support
            activity.window.decorView.viewTreeObserver.addOnGlobalLayoutListener(
                KeyboardGlobalLayoutListener(activity, eventBus)
            )
        }
    }

    @Subscribe
    @Deprecated("Use onGetKeyboardHeight")
    open fun onGetKeyboardStatus(event: GetKeyboardStatusEvent) {
        event.respond(GetKeyboardStatusEvent.ResultEvent(KeyboardStatus(getKeyboardHeight(activity))))
    }

    @Subscribe
    open fun onGetKeyboardHeight(event: GetKeyboardHeightEvent) {
        event.respond(GetKeyboardHeightEvent.ResultEvent(getKeyboardHeight(activity)))
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
            startHeight = getKeyboardHeight(activity)
        }
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ): WindowInsetsAnimationCompat.BoundsCompat {
        if (animation.typeMask and Type.ime() != 0) {
            endHeight = getKeyboardHeight(activity)
            eventBus.post(KeyboardAnimationStartedEvent(getKeyboardAnimation(animation)))
            eventBus.post(KeyboardStatusChangedEvent(KeyboardStatus(endHeight)))
        }
        return bounds
    }

    override fun onProgress(
        windowInsets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        return windowInsets
    }

    private fun getKeyboardAnimation(animation: WindowInsetsAnimationCompat): TweenAnimation {
        val durationScale =
            Settings.Global.getFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)

        val duration = (animation.durationMillis * durationScale).toInt()

        val easing = animation.interpolator
            ?.let {
                val frameCount = (FRAMES_PER_SECOND * duration / 1000).coerceIn(2, MAX_FRAME_COUNT)

                FloatArray(frameCount).apply {
                    forEachIndexed { index, _ ->
                        this[index] = (it.getInterpolation(index / (frameCount - 1f)) * 1000).toInt() / 1000f
                    }
                }
            }
            ?: floatArrayOf(0f, 1f)

        return TweenAnimation(startHeight, endHeight, duration, easing)
    }
}

private class KeyboardGlobalLayoutListener(private val activity: Activity, private val eventBus: EventBus) :
    ViewTreeObserver.OnGlobalLayoutListener {

    private var prevHeight = 0f

    override fun onGlobalLayout() {
        val height = getKeyboardHeight(activity)

        // Safe to compare because of the stable arithmetic
        if (prevHeight == height) {
            return
        }

        eventBus.post(KeyboardAnimationStartedEvent(TweenAnimation(prevHeight, height, 0, floatArrayOf(0f, 1f))))
        eventBus.post(KeyboardStatusChangedEvent(KeyboardStatus(getKeyboardHeight(activity))))

        prevHeight = height
    }
}

private fun getKeyboardHeight(activity: Activity): Float {
    val windowInsets = ViewCompat.getRootWindowInsets(activity.window.decorView) ?: return 0f

    return windowInsets.getInsets(Type.ime()).bottom / activity.resources.displayMetrics.density
}
