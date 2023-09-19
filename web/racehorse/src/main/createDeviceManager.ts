import { pickLocale } from 'locale-matcher';

import { EventBridge } from './createEventBridge';

export interface DeviceInfo {
  /**
   * [The SDK version of the software](https://apilevels.com) currently running on this hardware device. This value
   * never changes while a device is booted, but it may increase when the hardware manufacturer provides an OTA update.
   *
   * @see [Build.VERSION.SDK_INT](https://developer.android.com/reference/android/os/Build.VERSION#SDK_INT)
   */
  apiLevel: number;

  /**
   * The consumer-visible brand with which the product/hardware will be associated, if any.
   *
   * @see [Build.BRAND](https://developer.android.com/reference/android/os/Build#BRAND)
   */
  brand: string;

  /**
   * The end-user-visible name for the end product.
   *
   * @see [Build.MODEL](https://developer.android.com/reference/android/os/Build#MODEL)
   */
  model: string;
}

export interface Rect {
  top: number;
  left: number;
  bottom: number;
  right: number;
}

/**
 * @see [WindowInsetsCompat.Type](https://developer.android.com/reference/androidx/core/view/WindowInsetsCompat.Type)
 */
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

export interface DeviceManager {
  /**
   * Get OS and device versions.
   */
  getDeviceInfo(): DeviceInfo;

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

  /**
   * Get the rect that describes the window insets that overlap with system UI.
   *
   * @param typeMask Bit mask of {@link InsetType}s to query the insets for. By default, display cutout, navigation and
   * status bars are included.
   */
  getWindowInsets(typeMask?: number): Rect;
}

/**
 * Device configuration and general device information.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createDeviceManager(eventBridge: EventBridge): DeviceManager {
  const getPreferredLocales = () =>
    eventBridge.request({ type: 'org.racehorse.GetPreferredLocalesEvent' }).payload.locales;

  return {
    getDeviceInfo: () => eventBridge.request({ type: 'org.racehorse.GetDeviceInfoEvent' }).payload.deviceInfo,

    getPreferredLocales,

    pickLocale: (supportedLocales, defaultLocale) => pickLocale(getPreferredLocales(), supportedLocales, defaultLocale),

    getWindowInsets: (typeMask = InsetType.DISPLAY_CUTOUT | InsetType.NAVIGATION_BARS | InsetType.STATUS_BARS) =>
      eventBridge.request({ type: 'org.racehorse.GetWindowInsetsEvent', payload: { typeMask } }).payload.rect,
  };
}
