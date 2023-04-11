import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface GooglePlayReferrerManager {
  getGooglePlayReferrer(): Promise<string>;
}

/**
 * Gets [Google Play referrer](https://developer.android.com/google/play/installreferrer/library) information.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createGooglePlayReferrerManager(eventBridge: EventBridge): GooglePlayReferrerManager {
  let referrerPromise: Promise<string> | undefined;

  return {
    getGooglePlayReferrer() {
      return (referrerPromise ||= new Promise(resolve => {
        const unsubscribe = eventBridge.subscribe(event => {
          if (event.type === 'org.racehorse.GooglePlayReferrerDetectedEvent') {
            resolve(event.referrer);
            unsubscribe();
          }
        });

        eventBridge.request({ type: 'org.racehorse.GetGooglePlayReferrerRequestEvent' }).then(event => {
          if (ensureEvent(event).referrer) {
            resolve(event.referrer);
            unsubscribe();
          }
        });
      }));
    },
  };
}
