import { EventBridge } from './types';

export interface GooglePlayReferrerManager {
  getGooglePlayReferrer(): Promise<string>;
}

/**
 * Gets Google Play referrer information.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createGooglePlayReferrerManager(eventBridge: EventBridge): GooglePlayReferrerManager {
  let referrerPromise: Promise<string> | undefined;

  return {
    getGooglePlayReferrer() {
      return (referrerPromise ||= new Promise(resolve => {
        const unsubscribe = eventBridge.subscribe(event => {
          if (event.type === 'org.racehorse.GooglePlayReferrerDetectedAlertEvent') {
            resolve(event.referrer);
            unsubscribe();
          }
        });

        const referrer = eventBridge.requestSync({ type: 'org.racehorse.GetGooglePlayReferrerRequestEvent' })?.referrer;

        if (referrer) {
          resolve(referrer);
          unsubscribe();
        }
      }));
    },
  };
}
