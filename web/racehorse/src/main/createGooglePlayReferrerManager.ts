import { EventBridge } from './createEventBridge';

export interface GooglePlayReferrerManager {
  getGooglePlayReferrer(): Promise<string>;
}

/**
 * Gets [Google Play referrer](https://developer.android.com/google/play/installreferrer/library) information.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createGooglePlayReferrerManager(eventBridge: EventBridge): GooglePlayReferrerManager {
  let promise: Promise<string> | undefined;

  return {
    getGooglePlayReferrer: () =>
      (promise ||= eventBridge
        .requestAsync({ type: 'org.racehorse.GetGooglePlayReferrerEvent' })
        .then(event => event.payload.referrer)),
  };
}
