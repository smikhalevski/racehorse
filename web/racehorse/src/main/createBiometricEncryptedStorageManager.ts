import { EventBridge } from './createEventBridge';
import { BiometricAuthenticator } from './createBiometricManager';

export interface BiometricConfig {
  title?: string;
  subtitle?: string;
  description?: string;
  negativeButtonText?: string;
  authenticators?: BiometricAuthenticator[];
}

export interface BiometricEncryptedStorageManager {
  /**
   * Associates a value with a key in an encrypted storage.
   *
   * @param key A key to set.
   * @param value A value to write.
   * @param config The options of the biometric prompt.
   * @return `true` if the value was written to the storage, or `false` otherwise.
   */
  set(key: string, value: string, config?: BiometricConfig): Promise<boolean>;

  /**
   * Retrieves an encrypted value associated with the key.
   *
   * @returns The deciphered value, or `null` if key wasn't found or if password is incorrect.
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

export function createBiometricEncryptedStorageManager(eventBridge: EventBridge): BiometricEncryptedStorageManager {
  return {
    set: (key, value, config) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.SetBiometricEncryptedValueEvent', payload: { key, value, config } })
        .then(event => event.payload.isSuccessful),

    get: (key, config) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.GetBiometricEncryptedValueEvent', payload: { key, config } })
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
