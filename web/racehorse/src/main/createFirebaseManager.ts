import { EventBridge } from './createEventBridge';

export interface FirebaseManager {
  /**
   * Returns the Firebase token associated with the device.
   */
  getFirebaseToken(): string | null;
}

export function createFirebaseManager(eventBridge: EventBridge): FirebaseManager {
  return {
    getFirebaseToken: () => eventBridge.request({ type: 'org.racehorse.GetFirebaseTokenEvent' }).payload.token,
  };
}
