import { pickLocale } from 'locale-matcher';
import { EventBridge } from './createEventBridge';

/**
 * Provides access to application and device configuration.
 */
export interface ConfigurationManager {
  /**
   * Returns an array of preferred locales.
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
 * Creates the new {@linkcode ConfigurationManager} instance.
 *
 * @param eventBridge The event bridge to use for communication with Android device.
 */
export function createConfigurationManager(eventBridge: EventBridge): ConfigurationManager {
  const getPreferredLocales: ConfigurationManager['getPreferredLocales'] = () => {
    return eventBridge.request({ type: 'org.racehorse.GetPreferredLocalesRequestEvent' }).then(event => event.locales);
  };

  return {
    getPreferredLocales,

    pickLocale(supportedLocales, defaultLocale) {
      return getPreferredLocales().then(preferredLocales =>
        pickLocale(preferredLocales, supportedLocales, defaultLocale)
      );
    },
  };
}
