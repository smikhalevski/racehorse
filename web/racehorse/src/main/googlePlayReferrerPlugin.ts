import { Plugin } from './shared-types';

export interface GooglePlayReferrerMixin {
  getGooglePlayReferrer(): Promise<string>;
}

/**
 * Gets Google Play referrer information.
 */
export const googlePlayReferrerPlugin: Plugin<GooglePlayReferrerMixin> = eventBridge => {
  let referrerPromise: Promise<string> | undefined;

  eventBridge.getGooglePlayReferrer = () => {
    return (referrerPromise ||= new Promise(resolve => {
      const unsubscribe = eventBridge.watchForAlerts(event => {
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
    }));
  };
};
