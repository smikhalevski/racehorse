import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface EncryptedStorageManager {
  set(key: string, value: string, password: string): Promise<void>;

  get(key: string, password: string): Promise<string | null>;

  has(key: string): Promise<void>;

  delete(key: string): Promise<void>;
}

export function createEncryptedStorageManager(eventBridge: EventBridge): EncryptedStorageManager {
  return {
    set: (key, value, password) =>
      eventBridge.request({ type: 'org.racehorse.SetEncryptedValueRequestEvent', key, value, password }).then(event => {
        ensureEvent(event);
      }),

    get: (key, password) =>
      eventBridge
        .request({ type: 'org.racehorse.GetEncryptedValueRequestEvent', key, password })
        .then(event => ensureEvent(event).value),

    has: key =>
      eventBridge
        .request({ type: 'org.racehorse.HasEncryptedValueRequestEvent', key })
        .then(event => ensureEvent(event).exists),

    delete: key =>
      eventBridge.request({ type: 'org.racehorse.DeleteEncryptedValueRequestEvent', key }).then(event => {
        ensureEvent(event);
      }),
  };
}
