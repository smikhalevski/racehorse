import { EventBridge } from './types';
import { ensureEvent } from './utils';
import { Intent } from './createActivityManager';

export interface DeepLinkManager {
  /**
   * Returns the last deep link intent that was requested.
   */
  getLastDeepLink(): Intent | null;

  /**
   * Subscribes to incoming deep link intents.
   */
  subscribe(listener: (intent: Intent) => void): () => void;
}

/**
 * Monitors deep link requests.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createDeepLinkManager(eventBridge: EventBridge): DeepLinkManager {
  return {
    getLastDeepLink: () =>
      ensureEvent(eventBridge.requestSync({ type: 'org.racehorse.GetLastDeepLinkEvent' })).payload.intent,

    subscribe: listener =>
      eventBridge.subscribe(event => {
        if (event.type === 'org.racehorse.OpenDeepLinkEvent') {
          listener(event.payload.intent);
        }
      }),
  };
}
