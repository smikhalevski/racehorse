import { EventBridge } from './createEventBridge';
import { BiometricAuthenticator } from './createBiometricManager';
import { Scheduler } from './createScheduler';

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
}

export interface BiometricEncryptedStorageManager {
  /**
   * Associates a value with a key in an encrypted storage.
   *
   * **Note:** This is a UI-blocking operation. All consequent UI operations are suspended until this one is completed.
   *
   * @param key A key to set.
   * @param value A value to write.
   * @param config The options of the biometric prompt.
   * @return `true` if the value was written to the storage, or `false` if authentication has failed.
   * @throws InvalidAlgorithmParameterException At least one biometric must be enrolled to create keys requiring user
   * authentication for every use. Check {@link BiometricManager.getBiometricStatus} before setting the key.
   */
  set(key: string, value: string, config?: BiometricConfig): Promise<boolean>;

  /**
   * Retrieves an encrypted value associated with the key.
   *
   * **Note:** This is a UI-blocking operation. All consequent UI operations are suspended until this one is completed.
   *
   * @returns The deciphered value, or `null` if key wasn't found, or authentication has failed.
   * @throws InvalidAlgorithmParameterException At least one biometric must be enrolled to create keys requiring user
   * authentication for every use. Check {@link BiometricManager.getBiometricStatus} before reading the key.
   * @throws KeyPermanentlyInvalidatedException Indicates that the key can no longer be read because it has been
   * permanently invalidated (for example, because of the biometric enrollment). Set a new value or delete the key to
   * recover from this error.
   */
  get(key: string, config?: BiometricConfig): Promise<string | null>;

  /**
   * Returns `true` if the key exists in the storage, or `false` otherwise.
   */
  has(key: string): boolean;

  /**
   * Deletes the encrypted value associated with the key.
   *
   * @returns `true` if the key was deleted, or `false` if the key didn't exist.
   */
  delete(key: string): boolean;
}

/**
 * A biometric encrypted key-value file-based storage.
 *
 * @param eventBridge The underlying event bridge.
 * @param uiScheduler The callback that schedules an operation that blocks the UI.
 */
export function createBiometricEncryptedStorageManager(
  eventBridge: EventBridge,
  uiScheduler: Scheduler
): BiometricEncryptedStorageManager {
  return {
    set: (key, value, config) =>
      uiScheduler.schedule(() =>
        eventBridge
          .requestAsync({ type: 'org.racehorse.SetBiometricEncryptedValueEvent', payload: { key, value, config } })
          .then(event => event.payload.isSuccessful)
      ),

    get: (key, config) =>
      uiScheduler.schedule(() =>
        eventBridge
          .requestAsync({ type: 'org.racehorse.GetBiometricEncryptedValueEvent', payload: { key, config } })
          .then(event => event.payload.value)
      ),

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
