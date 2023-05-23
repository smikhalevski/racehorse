import { EventBridge } from './createEventBridge';

export interface EncryptedStorageManager {
  /**
   * Associates a value with a key in an encrypted storage.
   *
   * @param key A key to set. Must be a valid file name.
   * @param value A value to write to the file.
   * @param password The password that is used to cipher the file contents.
   */
  set(key: string, value: string, password: string): Promise<void>;

  /**
   * Retrieves an encrypted value associated with the key.
   *
   * @returns The deciphered value, or `null` if key wasn't found or if password is incorrect.
   */
  get(key: string, password: string): Promise<string | null>;

  /**
   * Checks that the key exists in the storage.
   */
  has(key: string): void;

  /**
   * Deletes an encrypted value associated with the key.
   */
  delete(key: string): void;
}

export function createEncryptedStorageManager(eventBridge: EventBridge): EncryptedStorageManager {
  return {
    set: (key, value, password) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.SetEncryptedValueEvent', payload: { key, value, password } })
        .then(() => undefined),

    get: (key, password) =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.GetEncryptedValueEvent', payload: { key, password } })
        .then(event => event.payload.value),

    has: key => eventBridge.request({ type: 'org.racehorse.HasEncryptedValueEvent', payload: { key } }).payload.exists,

    delete: key => {
      eventBridge.request({ type: 'org.racehorse.DeleteEncryptedValueEvent', payload: { key } });
    },
  };
}
