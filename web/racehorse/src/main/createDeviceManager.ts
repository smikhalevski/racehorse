import { pickLocale } from 'locale-matcher';
import { EventBridge } from './types';

export interface DeviceManager {
  /**
   * Returns the array of preferred locales.
   */
  getPreferredLocales(): string[];

  /**
   * Returns a locale from `supportedLocales` that best matches one of preferred locales, or returns a `defaultLocale`.
   *
   * @param supportedLocales The list of locales that your application supports.
   * @param defaultLocale The default locale that is returned if there's no matching locale among preferred.
   */
  pickLocale(supportedLocales: string[], defaultLocale: string): string;
}

/**
 * Device configuration and general information.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createDeviceManager(eventBridge: EventBridge): DeviceManager {
  const getPreferredLocales = () =>
    eventBridge.requestSync({ type: 'org.racehorse.GetPreferredLocalesRequestEvent' })?.locales || [];

  return {
    getPreferredLocales,

    pickLocale(supportedLocales, defaultLocale) {
      return pickLocale(getPreferredLocales(), supportedLocales, defaultLocale);
    },
  };
}
