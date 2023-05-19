import { EventBridge } from './types';
import { ensureEvent } from './utils';
import { Intent } from './createActivityManager';

export interface DeepLinkManager {
  /**
   * Returns the last deep link intent that was requested.
   */
  getLastDeepLink(): Promise<Intent | null>;

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
      eventBridge
        .request({ type: 'org.racehorse.GetLastDeepLinkEvent' })
        .then(event => ensureEvent(event).payload.intent),

    subscribe: listener =>
      eventBridge.subscribe(event => {
        if (event.type === 'org.racehorse.OpenDeepLinkEvent') {
          listener(event.payload.intent);
        }
      }),
  };
}
