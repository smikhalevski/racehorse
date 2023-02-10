import { pickLocale } from 'locale-matcher';
import { Plugin } from './shared-types';

export interface DeviceMixin {
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
 * Device configuration and general information.
 */
export const devicePlugin: Plugin<DeviceMixin> = eventBridge => {
  const getPreferredLocales: DeviceMixin['getPreferredLocales'] = () => {
    return eventBridge.request({ type: 'org.racehorse.GetPreferredLocalesRequestEvent' }).then(event => event.locales);
  };

  eventBridge.getPreferredLocales = getPreferredLocales;

  eventBridge.pickLocale = (supportedLocales, defaultLocale) => {
    return getPreferredLocales().then(preferredLocales =>
      pickLocale(preferredLocales, supportedLocales, defaultLocale)
    );
  };
};
