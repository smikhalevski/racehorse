import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface OpenUrlManager {
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
export function createOpenUrlManager(eventBridge: EventBridge): OpenUrlManager {
  return {
    openUrl: url =>
      eventBridge
        .request({ type: 'org.racehorse.OpenUrlRequestEvent', url })
        .then(event => ensureEvent(event).isOpened),
  };
}
