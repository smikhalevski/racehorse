import { EventBridge } from './createEventBridge';
import { noop } from './utils';
import { BiometricAuthenticator } from './createBiometricManager';

export interface BiometricPromptOptions {
  title?: string;
  subtitle?: string;
  negativeButtonText?: string;
  authenticators?: BiometricAuthenticator[];
}

export interface BiometricEncryptedStorageManager {
  /**
   * Associates a value with a key in an encrypted storage.
   *
   * @param key A key to set.
   * @param value A value to write.
   * @param options The options of the biometric prompt.
   */
  set(key: string, value: string, options?: BiometricPromptOptions): Promise<void>;

  /**
   * Retrieves an encrypted value associated with the key.
   *
   * @returns The deciphered value, or `null` if key wasn't found or if password is incorrect.
   */
  get(key: string, options?: BiometricPromptOptions): Promise<string | null>;

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

export function createBiometricEncryptedStorageManager(eventBridge: EventBridge): BiometricEncryptedStorageManager {
  return {
    set: (key, value, options) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.SetBiometricEncryptedValueEvent', payload: { key, value, options } })
        .then(noop),

    get: (key, options) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.GetBiometricEncryptedValueEvent', payload: { key, options } })
        .then(event => event.payload.value),

    has: key =>
      eventBridge.request({
        type: 'org.racehorse.HasBiometricEncryptedValueEvent',
        payload: { key },
      }).payload.exists,

    delete: key =>
      eventBridge.request({
        type: 'org.racehorse.DeleteBiometricEncryptedValueEvent',
        payload: { key },
      }).payload.deleted,
  };
}
