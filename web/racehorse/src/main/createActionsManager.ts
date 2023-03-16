import { EventBridge } from './types';

export interface ActionsManager {
  /**
   * Opens a URL in an external application.
   *
   * @param url The URL to open.
   * @returns `true` if external application was opened, or `false` otherwise.
   */
  openUrl(url: string): Promise<boolean>;
}

/**
 * Launches activities for various intents.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createActionsManager(eventBridge: EventBridge): ActionsManager {
  return {
    openUrl(url) {
      return eventBridge.request({ type: 'org.racehorse.OpenUrlRequestEvent', url }).then(event => event.opened);
    },
  };
}
