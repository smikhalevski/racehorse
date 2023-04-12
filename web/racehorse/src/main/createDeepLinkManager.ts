import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface DeepLinkManager {
  /**
   * Returns the last deep link that was requested.
   */
  getLastDeepLink(): Promise<string | null>;

  /**
   * Subscribes to incoming deep links.
   */
  subscribe(listener: (url: string) => void): () => void;
}

/**
 * Monitors deep link requests.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createDeepLinkManager(eventBridge: EventBridge): DeepLinkManager {
  return {
    getLastDeepLink: () =>
      eventBridge.request({ type: 'org.racehorse.GetLastDeepLinkRequestEvent' }).then(event => ensureEvent(event).url),

    subscribe: listener =>
      eventBridge.subscribe(event => {
        if (event.type === 'org.racehorse.OpenDeepLinkEvent') {
          listener(event.url);
        }
      }),
  };
}
