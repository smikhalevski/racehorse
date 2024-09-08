package org.racehorse

import android.app.Activity
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsCompat.Type
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.contentView
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

/**
 * Monitors keyboard visibility.
 *
 * @param activity The activity to which the keyboard observer is attached.
 * @param eventBus The event bus to which events are posted.
 */
open class KeyboardPlugin(private val activity: Activity, private val eventBus: EventBus = EventBus.getDefault()) {

    private val inputMethodManager by lazy { activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager }

    open fun enable() {
        ViewCompat.setWindowInsetsAnimationCallback(activity.contentView, KeyboardAnimationCallback(activity, eventBus))
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
        private const val MAX_ORDINATE_COUNT = 200L
    }

    private var startKeyboardHeight = 0f
    private var endKeyboardHeight = 0f

    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        if (animation.typeMask and Type.ime() == 0) {
            // Not a keyboard
            return
        }
        startKeyboardHeight = getKeyboardHeight(activity)
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: WindowInsetsAnimationCompat.BoundsCompat
    ): WindowInsetsAnimationCompat.BoundsCompat {
        if (animation.typeMask and Type.ime() == 0) {
            // Not a keyboard
            return bounds
        }

        endKeyboardHeight = getKeyboardHeight(activity)

        // Serialize interpolator as a set of control points
        val ordinateCount = min(FRAMES_PER_SECOND * animation.durationMillis / 1000, MAX_ORDINATE_COUNT)

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
                getKeyboardStatus(activity),
                max(startKeyboardHeight, endKeyboardHeight),
                animation.durationMillis,
                ordinates
            )
        )

        return bounds
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        if (animation.typeMask and Type.ime() == 0) {
            // Not a keyboard
            return
        }

        eventBus.post(AfterKeyboardStatusChangeEvent(getKeyboardStatus(activity)))
    }

    override fun onProgress(
        windowInsets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>
    ): WindowInsetsCompat {
        return windowInsets
    }
}

private fun getKeyboardHeight(activity: Activity): Float {
    val windowInsets = ViewCompat.getRootWindowInsets(activity.contentView)
        ?: return 0f

    return windowInsets.getInsets(Type.ime()).bottom / activity.resources.displayMetrics.density
}

private fun getKeyboardStatus(activity: Activity): KeyboardStatus {
    val windowInsets = ViewCompat.getRootWindowInsets(activity.contentView)
        ?: return KeyboardStatus(0f, false)

    return KeyboardStatus(
        windowInsets.getInsets(Type.ime()).bottom / activity.resources.displayMetrics.density,
        windowInsets.isVisible(Type.ime())
    )
}
