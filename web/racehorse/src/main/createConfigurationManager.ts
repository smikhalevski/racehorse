import { pickLocale } from 'locale-matcher';
import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface Rect {
  top: number;
  left: number;
  bottom: number;
  right: number;
}

export interface ConfigurationManager {
  getWindowInsets(): Promise<Rect>;

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

  /**
   * Subscribes a listener to soft keyboard visibility changes.
   */
  subscribeToKeyboardVisibility(listener: (keyboardVisible: boolean) => void): () => void;
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
    getWindowInsets() {
      return eventBridge
        .request({ type: 'org.racehorse.GetWindowInsetsRequestEvent' })
        .then(event => ensureEvent(event).rect);
    },

    subscribeToKeyboardVisibility(listener) {
      return eventBridge.subscribe(event => {
        if (event.type === 'org.racehorse.KeyboardVisibilityChangedAlertEvent') {
          listener(event.isKeyboardVisible);
        }
      });
    },

    getPreferredLocales,

    pickLocale(supportedLocales, defaultLocale) {
      return getPreferredLocales().then(locales => pickLocale(locales, supportedLocales, defaultLocale));
    },
  };
}
