import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface FirebaseManager {
  /**
   * Returns the Firebase token associated with the device.
   */
  getFirebaseToken(): Promise<string | null>;
}

export function createFirebaseManager(eventBridge: EventBridge): FirebaseManager {
  return {
    getFirebaseToken() {
      return eventBridge
        .request({ type: 'org.racehorse.GetFirebaseTokenRequestEvent' })
        .then(event => ensureEvent(event).token);
    },
  };
}