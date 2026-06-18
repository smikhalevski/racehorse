import { EventBridge } from './createEventBridge.js';
import { BiometricAuthenticator } from './createBiometricManager.js';

export interface BiometricConfig {
  /**
   * The title of the authentication popup.
   */
  title?: string;

  /**
   * The subtitle of the authentication popup.
   */
  subtitle?: string;

  /**
   * The description label of the authentication popup.
   */
  description?: string;

  /**
   * The label on the button that aborts the authentication.
   */
  negativeButtonText?: string;

  /**
   * The list of allowed authenticators.
   *
   * @default [BiometricAuthenticator.STRONG]
   */
  authenticators?: BiometricAuthenticator[];

  /**
   * The duration in seconds for which this key is authorized to be used after the user is successfully authenticated or
   * -1 if user authentication is required every time the key is accessed.
   *
   * If value is greater or equal to 0, then key won't be invalidated by biometric enrollment.
   *
   * **Note:** This option is only applicable when the key is set for the first time. To change this option, delete and
   * set the key again with the updated config.
   *
   * @default -1
   */
  authenticationValidityDuration?: number;
}

export const BiometricStorageErrorCode = {
  /**
   * Unknown error.
   */
  UNKNOWN: 'unknown',

  /**
   * Underlying file storage failed.
   */
  STORAGE_FAILED: 'storage_failed',

  /**
   * Encryption key became unrecoverable because user enrolled new auth method.
   */
  KEY_UNRECOVERABLE: 'key_unrecoverable',

  /**
   * The hardware is unavailable. Try again later.
   */
  HARDWARE_UNAVAILABLE: 'hardware_unavailable',

  /**
   * The sensor was unable to process the current image.
   */
  UNABLE_TO_PROCESS: 'unable_to_process',

  /**
   * The current operation has been running too long and has timed out.
   *
   * This is intended to prevent programs from waiting for the biometric sensor indefinitely.
   * The timeout is platform and sensor-specific, but is generally on the order of ~30 seconds.
   */
  TIMEOUT: 'timeout',

  /**
   * The operation can't be completed because there is not enough device storage remaining.
   */
  NO_SPACE: 'no_space',

  /**
   * The operation was canceled because the biometric sensor is unavailable. This may happen when
   * the user is switched, the device is locked, or another pending operation prevents it.
   */
  CANCELED: 'canceled',

  /**
   * The operation was canceled because the API is locked out due to too many attempts. This occurs after 5 failed
   * attempts, and lasts for 30 seconds.
   */
  LOCKOUT: 'lockout',

  /**
   * The operation failed due to a vendor-specific error.
   *
   * This error code may be used by hardware vendors to extend this list to cover errors that don't fall under one of
   * the other predefined categories. Vendors are responsible for providing the strings for these errors.
   *
   * These messages are typically reserved for internal operations such as enrollment but may be used to express any
   * error that is not otherwise covered. In this case, applications are expected to show the error message,
   * but they are advised not to rely on the message ID, since this may vary by vendor and device.
   */
  VENDOR: 'vendor',

  /**
   * The operation was canceled because {@link LOCKOUT} occurred too many times. Biometric authentication is disabled
   * until the user unlocks with their device credential (i.e. PIN, pattern, or password).
   */
  LOCKOUT_PERMANENT: 'lockout_permanent',

  /**
   * The user canceled the operation by pressing "Back" button.
   *
   * Upon receiving this, applications should use alternate authentication, such as a password.
   *
   * The application should also provide the user a way of returning to biometric authentication, such as a button.
   */
  USER_CANCELED: 'user_canceled',

  /**
   * The user does not have any biometrics enrolled.
   */
  NO_BIOMETRICS: 'no_biometrics',

  /**
   * The device does not have the required authentication hardware.
   */
  HARDWARE_NOT_PRESENT: 'hardware_not_present',

  /**
   * The user pressed the negative button.
   *
   * @see {@link BiometricConfig.negativeButtonText}
   */
  NEGATIVE_BUTTON: 'negative_button',

  /**
   * The device does not have pin, pattern, or password set up.
   */
  NO_DEVICE_CREDENTIAL: 'no_device_credential',

  /**
   * A security vulnerability has been discovered with one or more hardware sensors. The affected sensor(s)
   * are unavailable until a security update has addressed the issue.
   */
  SECURITY_UPDATE_REQUIRED: 'security_update_required',
} as const;

export type BiometricStorageErrorCode = (typeof BiometricStorageErrorCode)[keyof typeof BiometricStorageErrorCode];

export interface BiometricEncryptedStorageSetResult {
  /**
   * `true` if the value was written to the storage, or `false` if authentication has failed.
   */
  isSuccessful: boolean;

  /**
   * The error that prevented from successfully writing the value to the storage, or `null` if there was no error.
   */
  errorCode: BiometricStorageErrorCode | null;
}

export interface BiometricEncryptedStorageGetResult {
  /**
   * The deciphered value, or `null` if key wasn't found, or authentication has failed.
   */
  value: string | null;

  /**
   * The error that prevented from returning the value, or `null` if there was no error.
   */
  errorCode: BiometricStorageErrorCode | null;
}

export interface BiometricEncryptedStorageManager {
  /**
   * Associates a value with a key in an encrypted storage.
   *
   * **Note:** This operation requires the user interaction, consider using {@link ActivityManager.runUserInteraction}
   * to ensure that consequent UI-related operations are suspended until this one is completed.
   *
   * @param key A key to set.
   * @param value A value to write.
   * @param config The options of the biometric prompt.
   */
  set(key: string, value: string, config?: BiometricConfig): Promise<BiometricEncryptedStorageSetResult>;

  /**
   * Retrieves an encrypted value associated with the key.
   *
   * **Note:** This operation requires the user interaction, consider using {@link ActivityManager.runUserInteraction}
   * to ensure that consequent UI-related operations are suspended until this one is completed.
   *
   * @param key A key to get value for.
   * @param config The options of the biometric prompt.
   */
  get(key: string, config?: BiometricConfig): Promise<BiometricEncryptedStorageGetResult>;

  /**
   * Returns `true` if the key exists in the storage, or `false` otherwise.
   */
  has(key: string): boolean;

  /**
   * Deletes the key and the associated encrypted value.
   *
   * @returns `true` if the key was deleted, or `false` if the key didn't exist.
   */
  delete(key: string): boolean;
}

/**
 * A biometric encrypted key-value file-based storage.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createBiometricEncryptedStorageManager(eventBridge: EventBridge): BiometricEncryptedStorageManager {
  return {
    set: (key, value, config) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.SetBiometricEncryptedValueEvent', payload: { key, value, config } })
        .then(event => event.payload),

    get: (key, config) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.GetBiometricEncryptedValueEvent', payload: { key, config } })
        .then(event => event.payload),

    has: key =>
      eventBridge.request({
        type: 'org.racehorse.HasBiometricEncryptedValueEvent',
        payload: { key },
      }).payload.isExisting,

    delete: key =>
      eventBridge.request({
        type: 'org.racehorse.DeleteBiometricEncryptedValueEvent',
        payload: { key },
      }).payload.isDeleted,
  };
}
