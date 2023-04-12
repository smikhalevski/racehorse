import { pickLocale } from 'locale-matcher';
import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface Rect {
  top: number;
  left: number;
  bottom: number;
  right: number;
}

export const InsetType = {
  STATUS_BARS: 1,
  NAVIGATION_BARS: 1 << 1,
  CAPTION_BAR: 1 << 2,
  IME: 1 << 3,
  SYSTEM_GESTURES: 1 << 4,
  MANDATORY_SYSTEM_GESTURES: 1 << 5,
  TAPPABLE_ELEMENT: 1 << 6,
  DISPLAY_CUTOUT: 1 << 7,
  WINDOW_DECOR: 1 << 8,
} as const;

export interface ConfigurationManager {
  /**
   * Get the rect that describes the window insets that overlap with system UI.
   *
   * @param typeMask Bit mask of {@link InsetType}s to query the insets for. By default, display cutout, navigation and
   * status bars are included.
   */
  getWindowInsets(typeMask?: number): Promise<Rect>;

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
    getWindowInsets(typeMask) {
      return eventBridge
        .request({ type: 'org.racehorse.GetWindowInsetsRequestEvent', typeMask })
        .then(event => ensureEvent(event).rect);
    },

    subscribeToKeyboardVisibility(listener) {
      return eventBridge.subscribe(event => {
        if (event.type === 'org.racehorse.KeyboardVisibilityChangedEvent') {
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
