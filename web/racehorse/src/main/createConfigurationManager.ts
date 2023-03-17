import { pickLocale } from 'locale-matcher';
import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface ConfigurationManager {
  /**
   * Returns the array of preferred locales.
   */
  getPreferredLocales(): Promise<string[]>;

  /**
   * Returns a locale from `supportedLocales` that best matches one of preferred locales, or returns a `defaultLocale`.
   *
   * @param supportedLocales The list of locales that your application supports.
   * @param defaultLocale The default locale that is returned if there's no matching locale among preferred.
   */
  pickLocale(supportedLocales: string[], defaultLocale: string): Promise<string>;
}

/**
 * Device configuration and general information.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createConfigurationManager(eventBridge: EventBridge): ConfigurationManager {
  const getPreferredLocales = () =>
    eventBridge
      .request({ type: 'org.racehorse.GetPreferredLocalesRequestEvent' })
      .then(event => ensureEvent(event).locales);

  return {
    getPreferredLocales,

    pickLocale(supportedLocales, defaultLocale) {
      return getPreferredLocales().then(locales => pickLocale(locales, supportedLocales, defaultLocale));
    },
  };
}
