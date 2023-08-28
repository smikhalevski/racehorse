import { EventBridge } from './createEventBridge';
import { noop } from './utils';

export interface EncryptedStorageManager {
  /**
   * Associates a value with a key in an encrypted storage.
   *
   * @param key A key to set.
   * @param value A value to write.
   * @param password The password that is used to cipher the value.
   */
  set(key: string, value: string, password: string): Promise<void>;

  /**
   * Retrieves an encrypted value associated with the key.
   *
   * @returns The deciphered value, or `null` if key wasn't found or if password is incorrect.
   */
  get(key: string, password: string): Promise<string | null>;

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

export function createEncryptedStorageManager(eventBridge: EventBridge): EncryptedStorageManager {
  return {
    set: (key, value, password) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.SetEncryptedValueEvent', payload: { key, value, password } })
        .then(noop),

    get: (key, password) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.GetEncryptedValueEvent', payload: { key, password } })
        .then(event => event.payload.value),

    has: key => eventBridge.request({ type: 'org.racehorse.HasEncryptedValueEvent', payload: { key } }).payload.exists,

    delete: key =>
      eventBridge.request({
        type: 'org.racehorse.DeleteEncryptedValueEvent',
        payload: { key },
      }).payload.deleted,
  };
}
