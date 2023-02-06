import { EventBridge } from './createEventBridge';

/**
 * Launches activities for various intents.
 */
export interface IntentsManager {
  /**
   * Opens a URL in an external browser or another app that supports deep links with this URL.
   *
   * @param url The URL to open.
   * @returns `true` if external browser was opened, or `false` otherwise.
   */
  openInExternalActivity(url: string): Promise<boolean>;
}

/**
 * Creates the new {@linkcode IntentsManager} instance.
 *
 * @param eventBridge The event bridge to use for communication with Android device.
 */
export function createIntentsManager(eventBridge: EventBridge): IntentsManager {
  return {
    openInExternalActivity(url) {
      return eventBridge.request({ type: 'org.racehorse.OpenInExternalActivityEvent', url }).then(event => event.ok);
    },
  };
}
