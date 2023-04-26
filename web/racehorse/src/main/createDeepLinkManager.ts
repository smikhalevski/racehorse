import { EventBridge, WebIntent } from './types';
import { ensureEvent } from './utils';

export interface DeepLinkManager {
  /**
   * Returns the last deep link intent that was requested.
   */
  getLastDeepLink(): Promise<WebIntent | null>;

  /**
   * Subscribes to incoming deep link intents.
   */
  subscribe(listener: (intent: WebIntent) => void): () => void;
}

/**
 * Monitors deep link requests.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createDeepLinkManager(eventBridge: EventBridge): DeepLinkManager {
  return {
    getLastDeepLink: () =>
      eventBridge.request({ type: 'org.racehorse.GetLastDeepLinkEvent' }).then(event => ensureEvent(event).intent),

    subscribe: listener =>
      eventBridge.subscribe(event => {
        if (event.type === 'org.racehorse.OpenDeepLinkEvent') {
          listener(event.intent);
        }
      }),
  };
}
