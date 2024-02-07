package org.racehorse

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.fragment.app.FragmentActivity
import com.google.gson.annotations.SerializedName
import org.greenrobot.eventbus.Subscribe
import org.racehorse.utils.launchActivityForResult

enum class BiometricAuthenticator(val value: Int) {
    /**
     * Any biometric (e.g. fingerprint, iris, or face) on the device that meets or exceeds the requirements for
     * **Class 3**, as defined by the Android CDD.
     */
    @SerializedName("biometric_strong")
    BIOMETRIC_STRONG(BiometricManager.Authenticators.BIOMETRIC_STRONG),

    /**
     * Any biometric (e.g. fingerprint, iris, or face) on the device that meets or exceeds the requirements for
     * **Class 2**, as defined by the Android CDD.
     */
    @SerializedName("biometric_weak")
    BIOMETRIC_WEAK(BiometricManager.Authenticators.BIOMETRIC_WEAK),

    /**
     * The non-biometric credential used to secure the device (i.e. PIN, pattern, or password). This should typically
     * only be used in combination with a biometric auth type, such as [BIOMETRIC_WEAK].
     */
    @SerializedName("device_credential")
    DEVICE_CREDENTIAL(BiometricManager.Authenticators.DEVICE_CREDENTIAL);

    companion object {
        fun from(authenticators: Array<BiometricAuthenticator>?): Int {
            val result = authenticators?.map(BiometricAuthenticator::value)?.fold(0, Int::or) ?: 0

            return if (result == 0) BiometricManager.Authenticators.BIOMETRIC_STRONG else result
        }
    }
}

enum class BiometricStatus(val value: Int) {
    /**
     * App can authenticate using biometrics.
     */
    @SerializedName("supported")
    SUPPORTED(BiometricManager.BIOMETRIC_SUCCESS),

    /**
     * Unable to determine whether the user can authenticate.
     *
     * This status code may be returned on older Android versions due to partial incompatibility with a newer API.
     */
    @SerializedName("unknown")
    UNKNOWN(BiometricManager.BIOMETRIC_STATUS_UNKNOWN),

    /**
     * The user can't authenticate because the specified options are incompatible with the current Android version.
     */
    @SerializedName("unsupported")
    UNSUPPORTED(BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED),

    /**
     * No biometric features available on this device.
     */
    @SerializedName("no_hardware")
    NO_HARDWARE(BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE),

    /**
     * Biometric features are currently unavailable.
     */
    @SerializedName("hardware_unavailable")
    HARDWARE_UNAVAILABLE(BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE),

    /**
     * The user can't authenticate because no biometric or device credential is enrolled.
     */
    @SerializedName("none_enrolled")
    NONE_ENROLLED(BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED),

    /**
     * The user can't authenticate because a security vulnerability has been discovered with one or more hardware
     * sensors. The affected sensor(s) are unavailable until a security update has addressed the issue.
     */
    @SerializedName("security_update_required")
    SECURITY_UPDATE_REQUIRED(BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED);

    companion object {
        fun from(value: Int) = values().firstOrNull { it.value == value } ?: UNKNOWN
    }
}

class GetBiometricStatusEvent(val authenticators: Array<BiometricAuthenticator>?) : RequestEvent() {
    class ResultEvent(val status: BiometricStatus) : ResponseEvent()
}

class EnrollBiometricEvent(val authenticators: Array<BiometricAuthenticator>?) : RequestEvent() {
    class ResultEvent(val isEnrolled: Boolean) : ResponseEvent()
}

class BiometricPlugin(private val activity: FragmentActivity) {

    private val biometricManager by lazy { BiometricManager.from(activity) }

    @Subscribe
    fun onGetBiometricStatus(event: GetBiometricStatusEvent) {
        val status = biometricManager.canAuthenticate(BiometricAuthenticator.from(event.authenticators))

        event.respond(GetBiometricStatusEvent.ResultEvent(BiometricStatus.from(status)))
    }

    @Subscribe
    fun onEnrollBiometric(event: EnrollBiometricEvent) {
        val authenticators = BiometricAuthenticator.from(event.authenticators)
        val status = biometricManager.canAuthenticate(authenticators)

        if (status != BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            // Already enrolled, or cannot enroll due to an error
            event.respond(EnrollBiometricEvent.ResultEvent(status == BiometricManager.BIOMETRIC_SUCCESS))
            return
        }

        val enrollIntent = when {
            // Biometric
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                .putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, authenticators)

            // Fingerprint
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> Intent(Settings.ACTION_FINGERPRINT_ENROLL)

            // Open security settings, so user can navigate to Fingerprint section
            else -> Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

        val isLaunched = activity.launchActivityForResult(enrollIntent) {
            event.respond(EnrollBiometricEvent.ResultEvent(biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS))
        }

        if (!isLaunched) {
            event.respond(EnrollBiometricEvent.ResultEvent(false))
        }
    }
}
