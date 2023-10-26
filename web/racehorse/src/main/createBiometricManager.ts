import { EventBridge } from './createEventBridge';

/**
 * Types of authenticators, defined at a level of granularity supported by {@link BiometricManager}.
 */
export const BiometricAuthenticator = {
  /**
   * Any biometric (e.g. fingerprint, iris, or face) on the device that meets or exceeds the requirements for
   * **Class 3**, as defined by the Android CDD.
   */
  BIOMETRIC_STRONG: 'biometric_strong',

  /**
   * Any biometric (e.g. fingerprint, iris, or face) on the device that meets or exceeds the requirements for
   * **Class 2**, as defined by the Android CDD.
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
} as const;

export type BiometricStatus = (typeof BiometricStatus)[keyof typeof BiometricStatus];

export interface BiometricManager {
  /**
   * Returns the status of the biometric authentication support for a given set of authenticators.
   *
   * @param [authenticators = [BiometricAuthenticator.BIOMETRIC_WEAK]] The array of authenticators that must be supported
   * for successful result.
   */
  getBiometricStatus(authenticators?: BiometricAuthenticator[]): BiometricStatus;

  /**
   * Prompts the user to register credentials for given authenticators. If user already enrolled then returns a promise
   * without any user interaction.
   *
   * @param [authenticators = [BiometricAuthenticator.BIOMETRIC_WEAK]] The array of authenticators that must be supported
   * for successful enrollment.
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
    getBiometricStatus: (authenticators = [BiometricAuthenticator.BIOMETRIC_WEAK]) =>
      eventBridge.request({
        type: 'org.racehorse.GetBiometricStatusEvent',
        payload: { authenticators },
      }).payload.status,

    enrollBiometric: (authenticators = [BiometricAuthenticator.BIOMETRIC_WEAK]) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.EnrollBiometricEvent', payload: { authenticators } })
        .then(event => event.payload.isEnrolled),
  };
}
