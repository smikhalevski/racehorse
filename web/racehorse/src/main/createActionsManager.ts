import { EventBridge } from './createEventBridge';

/**
 * Launches activities for various intents.
 */
export interface ActionsManager {
  /**
   * Opens a URL in an external application.
   *
   * @param url The URL to open.
   * @returns `true` if external browser was opened, or `false` otherwise.
   */
  openUrl(url: string): Promise<boolean>;
}

/**
 * Creates the new {@linkcode ActionsManager} instance.
 *
 * @param eventBridge The event bridge to use for communication with Android device.
 */
export function createActionsManager(eventBridge: EventBridge): ActionsManager {
  return {
    openUrl(url) {
      return eventBridge.request({ type: 'org.racehorse.OpenUrlRequestEvent', url }).then(event => event.ok);
    },
  };
}
