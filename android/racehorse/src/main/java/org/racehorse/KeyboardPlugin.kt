package org.racehorse

import android.app.Activity
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.io.Serializable
import java.lang.Long.min
import kotlin.math.max
import kotlin.math.roundToInt

class KeyboardStatus(val height: Float, val isShown: Boolean) : Serializable {
    @Deprecated("Use isShown")
    val isVisible = isShown
}

/**
 * Notifies the web app that the keyboard status is about to be changed.
 */
class BeforeKeyboardStatusChangeEvent(
    val status: KeyboardStatus,
    val height: Float,
    val startTimestamp: Long,
    val animationDuration: Long,
    val ordinates: FloatArray?
) : NoticeEvent

/**
 * Notifies the web app that the keyboard status has changed.
 */
class AfterKeyboardStatusChangeEvent(val status: KeyboardStatus) : NoticeEvent

class GetKeyboardStatusEvent : RequestEvent() {
    class ResultEvent(val status: KeyboardStatus) : ResponseEvent()
}

class ShowKeyboardEvent : WebEvent

class HideKeyboardEvent : RequestEvent() {
    class ResultEvent(val isHidden: Boolean) : ResponseEvent()
}

private val KEYBOARD_TYPE_MASK =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) Type.ime()
    else Type.ime() or Type.systemBars()

/**
 * Monitors keyboard visibility.
 *
 * @param activity The activity to which the keyboard observer is attached.
 * @param eventBus The event bus to which events are posted.
 */
open class KeyboardPlugin(
    private val activity: Activity,
    private val view: View,
    private val eventBus: EventBus = EventBus.getDefault()
) {

    private val inputMethodManager by lazy { activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    open fun enable() {
        ViewCompat.setWindowInsetsAnimationCallback(view, KeyboardAnimationCallback(activity, eventBus))
    }

    @Subscribe
    open fun onGetKeyboardStatus(event: GetKeyboardStatusEvent) {
        event.respond(GetKeyboardStatusEvent.ResultEvent(getKeyboardStatus(activity)))
    }

    @Subscribe
    open fun onShowKeyboard(event: ShowKeyboardEvent) {
        inputMethodManager.showSoftInput(activity.currentFocus, 0)
    }

    @Subscribe
    open fun onHideKeyboard(event: HideKeyboardEvent) {
        event.respond(
            HideKeyboardEvent.ResultEvent(
                inputMethodManager.hideSoftInputFromWindow(
                    activity.currentFocus?.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            )
        )
    }
}

private class KeyboardAnimationCallback(private val activity: Activity, private val eventBus: EventBus) :
    WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE) {

    companion object {
        private const val FRAMES_PER_SECOND = 60
        private const val MAX_ORDINATE_COUNT = 400L
    }

    private val minKeyboardHeight = getKeyboardHeight(activity)

    private var startKeyboardHeight = 0f
    private var endKeyboardHeight = 0f

    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        if (animation.typeMask and KEYBOARD_TYPE_MASK == 0) {
            // Not a keyboard
            return
        }
        startKeyboardHeight = getKeyboardHeight(activity) - minKeyboardHeight
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ): WindowInsetsAnimationCompat.BoundsCompat {
        if (animation.typeMask and KEYBOARD_TYPE_MASK == 0) {
            // Not a keyboard
            return bounds
        }

        endKeyboardHeight = getKeyboardHeight(activity) - minKeyboardHeight

        val animationDurationScale =
            Settings.Global.getFloat(activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1.0f)

        val animationDuration = (animation.durationMillis * animationDurationScale).toLong()

        // Serialize interpolator as a set of equidistant ordinates
        val ordinateCount = min(FRAMES_PER_SECOND * animationDuration / 1000, MAX_ORDINATE_COUNT)

        val ordinates = animation.interpolator?.let { interpolator ->
            val ordinates = FloatArray(ordinateCount.toInt())

            ordinates.forEachIndexed { index, _ ->
                ordinates[index] =
                    (interpolator.getInterpolation(index.toFloat() / (ordinateCount - 1)) * 1000).roundToInt() / 1000f
            }
            ordinates
        }

        eventBus.post(
            BeforeKeyboardStatusChangeEvent(
                status = KeyboardStatus(endKeyboardHeight, endKeyboardHeight > 0),
                height = max(startKeyboardHeight, endKeyboardHeight),
                startTimestamp = System.currentTimeMillis(),
                animationDuration = animationDuration,
                ordinates = ordinates
            )
        )

        return bounds
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        if (animation.typeMask and KEYBOARD_TYPE_MASK == 0) {
            // Not a keyboard
            return
        }

        eventBus.post(AfterKeyboardStatusChangeEvent(KeyboardStatus(endKeyboardHeight, endKeyboardHeight > 0)))
    }

    override fun onProgress(
        windowInsets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        return windowInsets
    }
}

private fun getKeyboardHeight(activity: Activity): Float {
    val windowInsets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        ?: return 0f

    return windowInsets.getInsets(KEYBOARD_TYPE_MASK).bottom / activity.resources.displayMetrics.density
}

private fun getKeyboardStatus(activity: Activity): KeyboardStatus {
    val windowInsets = ViewCompat.getRootWindowInsets(activity.window.decorView)
        ?: return KeyboardStatus(0f, false)

    return KeyboardStatus(
        windowInsets.getInsets(KEYBOARD_TYPE_MASK).bottom / activity.resources.displayMetrics.density,
        windowInsets.isVisible(KEYBOARD_TYPE_MASK)
    )
}
