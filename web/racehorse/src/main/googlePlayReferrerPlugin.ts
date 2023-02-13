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
      const referrer = eventBridge.requestSync({ type: 'org.racehorse.GetGooglePlayReferrerRequestEvent' })?.referrer;

      if (referrer) {
        resolve(referrer);
      }

      const unsubscribe = eventBridge.subscribeToAlerts(event => {
        if (event.type === 'org.racehorse.GooglePlayReferrerDetectedAlertEvent') {
          resolve(event.referrer);
          unsubscribe();
        }
      });
    }));
  };
};
