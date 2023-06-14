import { EventBridge } from './createEventBridge';

export interface FirebaseManager {
  /**
   * Returns the Firebase token associated with the device.
   */
  getFirebaseToken(): Promise<string | null>;
}

export function createFirebaseManager(eventBridge: EventBridge): FirebaseManager {
  return {
    getFirebaseToken: () =>
      eventBridge.requestAsync({ type: 'org.racehorse.GetFirebaseTokenEvent' }).then(event => event.payload.token),
  };
}
