import { pickLocale } from 'locale-matcher';
import { Plugin } from './shared-types';

export interface DeviceMixin {
  /**
   * Returns an array of preferred locales.
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
 */
export const devicePlugin: Plugin<DeviceMixin> = eventBridge => {
  eventBridge.getPreferredLocales = () => {
    return eventBridge.requestSync({ type: 'org.racehorse.GetPreferredLocalesRequestEvent' })?.locales || [];
  };

  eventBridge.pickLocale = (supportedLocales, defaultLocale) => {
    return pickLocale(eventBridge.getPreferredLocales!(), supportedLocales, defaultLocale);
  };
};
