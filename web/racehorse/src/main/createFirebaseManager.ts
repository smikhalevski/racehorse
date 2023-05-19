import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface FirebaseManager {
  /**
   * Returns the Firebase token associated with the device.
   */
  getFirebaseToken(): string | null;
}

export function createFirebaseManager(eventBridge: EventBridge): FirebaseManager {
  return {
    getFirebaseToken: () =>
      ensureEvent(eventBridge.requestSync({ type: 'org.racehorse.GetFirebaseTokenEvent' })).payload.token,
  };
}
