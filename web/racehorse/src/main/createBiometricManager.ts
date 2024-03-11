import { EventBridge } from './createEventBridge';

/**
 * Types of authenticators, defined at a level of granularity supported by {@link BiometricManager}.
 */
export const BiometricAuthenticator = {
  /**
   * Any biometric (e.g. fingerprint, iris, or face) on the device that meets or exceeds the requirements for Class 3,
   * as defined by the Android CDD.
   */
  BIOMETRIC_STRONG: 'biometric_strong',

  /**
   * Any biometric (e.g. fingerprint, iris, or face) on the device that meets or exceeds the requirements for Class 2,
   * as defined by the Android CDD.
   */
  BIOMETRIC_WEAK: 'biometric_weak',

  /**
   * The non-biometric credential used to secure the device (i.e. PIN, pattern, or password). This should typically
   * only be used in combination with a biometric auth type, such as {@link BIOMETRIC_WEAK}.
   */
  DEVICE_CREDENTIAL: 'device_credential',
} as const;

export type BiometricAuthenticator = (typeof BiometricAuthenticator)[keyof typeof BiometricAuthenticator];

/**
 * Types of authenticators, defined at a level of granularity supported by {@link BiometricManager}.
 */
export const BiometricStatus = {
  /**
   * App can authenticate using biometrics.
   */
  SUPPORTED: 'supported',

  /**
   * Unable to determine whether the user can authenticate.
   *
   * This status code may be returned on older Android versions due to partial incompatibility with a newer API.
   */
  UNKNOWN: 'unknown',

  /**
   * The user can't authenticate because the specified options are incompatible with the current Android version.
   */
  UNSUPPORTED: 'unsupported',

  /**
   * No biometric features available on this device.
   */
  NO_HARDWARE: 'no_hardware',

  /**
   * Biometric features are currently unavailable.
   */
  HARDWARE_UNAVAILABLE: 'hardware_unavailable',

  /**
   * Biometric authentication isn't set up.
   */
  NONE_ENROLLED: 'none_enrolled',

  /**
   * The user can't authenticate because a security vulnerability has been discovered with one or more hardware sensors.
   * The affected sensor(s) are unavailable until a security update has addressed the issue.
   */
  SECURITY_UPDATE_REQUIRED: 'security_update_required',
} as const;

export type BiometricStatus = (typeof BiometricStatus)[keyof typeof BiometricStatus];

export interface BiometricManager {
  /**
   * Returns the status of the biometric authentication support for a given set of authenticators.
   *
   * @param authenticators The array of authenticators that must be supported for successful result. If omitted, or if
   * an empty array is provided then {@link BiometricAuthenticator.BIOMETRIC_STRONG} is used.
   */
  getBiometricStatus(authenticators?: BiometricAuthenticator[]): BiometricStatus;

  /**
   * Prompts the user to register credentials for given authenticators. If user already enrolled then returns a promise
   * without any user interaction.
   *
   * **Note:** This operation requires the user interaction, consider using {@link ActivityManager.runUserInteraction}
   * to ensure that consequent UI-related operations are suspended until this one is completed.
   *
   * @param authenticators The array of authenticators that must be supported for successful enrollment. If omitted, or
   * if an empty array is provided then {@link BiometricAuthenticator.BIOMETRIC_STRONG} is used.
   * @return `true` if biometric enrollment succeeded, or `false` otherwise.
   */
  enrollBiometric(authenticators?: BiometricAuthenticator[]): Promise<boolean>;
}

/**
 * Provides the status of biometric support and allows to enroll for biometric auth.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createBiometricManager(eventBridge: EventBridge): BiometricManager {
  return {
    getBiometricStatus: authenticators =>
      eventBridge.request({
        type: 'org.racehorse.GetBiometricStatusEvent',
        payload: { authenticators },
      }).payload.status,

    enrollBiometric: authenticators =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.EnrollBiometricEvent', payload: { authenticators } })
        .then(event => event.payload.isEnrolled),
  };
}
