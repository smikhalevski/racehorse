import { EventBridge } from './createEventBridge.js';

export interface FirebaseManager {
  /**
   * Returns the Firebase token associated with the device.
   */
  getFirebaseToken(): Promise<string | null>;
}

/**
 * Provides access to Firebase configuration.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createFirebaseManager(eventBridge: EventBridge): FirebaseManager {
  return {
    getFirebaseToken: () =>
      eventBridge.requestAsync({ type: 'org.racehorse.GetFirebaseTokenEvent' }).then(event => event.payload.token),
  };
}
