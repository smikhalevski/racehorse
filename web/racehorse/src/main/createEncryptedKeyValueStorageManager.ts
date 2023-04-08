import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface EncryptedKeyValueStorageManager {
  set(key: string, value: string, password: string): Promise<void>;

  get(key: string, password: string): Promise<string | null>;

  has(key: string): Promise<void>;

  delete(key: string): Promise<void>;
}

export function createEncryptedKeyValueStorageManager(eventBridge: EventBridge): EncryptedKeyValueStorageManager {
  return {
    set(key, value, password) {
      return eventBridge
        .request({ type: 'org.racehorse.SetEncryptedValueRequestEvent', key, value, password })
        .then(event => {
          ensureEvent(event);
        });
    },

    get(key, password) {
      return eventBridge
        .request({ type: 'org.racehorse.GetEncryptedValueRequestEvent', key, password })
        .then(event => ensureEvent(event).value);
    },

    has(key) {
      return eventBridge
        .request({ type: 'org.racehorse.HasEncryptedValueRequestEvent', key })
        .then(event => ensureEvent(event).exists);
    },

    delete(key) {
      return eventBridge.request({ type: 'org.racehorse.DeleteEncryptedValueRequestEvent', key }).then(event => {
        ensureEvent(event);
      });
    },
  };
}
