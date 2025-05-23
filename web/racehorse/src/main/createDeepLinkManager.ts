import { Intent } from './createActivityManager.js';
import { EventBridge } from './createEventBridge.js';
import { Unsubscribe } from './types.js';

export interface DeepLinkManager {
  /**
   * Returns the last deep link intent that was requested.
   */
  getLastDeepLink(): Intent | null;

  /**
   * Subscribes to incoming deep link intents.
   */
  subscribe(listener: (intent: Intent) => void): Unsubscribe;
}

/**
 * Monitors deep link requests.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createDeepLinkManager(eventBridge: EventBridge): DeepLinkManager {
  return {
    getLastDeepLink: () => eventBridge.request({ type: 'org.racehorse.GetLastDeepLinkEvent' }).payload.intent,

    subscribe: listener =>
      eventBridge.subscribe('org.racehorse.OpenDeepLinkEvent', payload => {
        listener(payload.intent);
      }),
  };
}
