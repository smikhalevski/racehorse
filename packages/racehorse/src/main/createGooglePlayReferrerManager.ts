import { EventBridge } from './createEventBridge';

export interface GooglePlayReferrerManager {
  getGooglePlayReferrer(): Promise<string>;
}

export function createGooglePlayReferrerManager(eventBridge: EventBridge): GooglePlayReferrerManager {
  let referrerPromise: Promise<string> | undefined;

  return {
    getGooglePlayReferrer() {
      referrerPromise ||= new Promise(resolve => {
        const unsubscribe = eventBridge.subscribe(event => {
          if (event.type === 'org.racehorse.GooglePlayReferrerDetectedAlertEvent') {
            resolve(event.referrer);
            unsubscribe();
          }
        });

        eventBridge.request({ type: 'org.racehorse.GetGooglePlayReferrerRequestEvent' }).then(event => {
          if (event.referrer) {
            resolve(event.referrer);
            unsubscribe();
          }
        });
      });

      return referrerPromise;
    },
  };
}