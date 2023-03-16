import { PubSub } from 'parallel-universe';
import { EventBridge } from './types';

export interface NetworkManager {
  /**
   * The current online status, or `undefined` if not yet known.
   */
  readonly online: boolean | undefined;

  /**
   * Subscribes to network status changes.
   */
  subscribe(listener: () => void): () => void;
}

/**
 * Monitors online status.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createNetworkManager(eventBridge: EventBridge): NetworkManager {
  const pubSub = new PubSub();

  let online: boolean | undefined;
  let unsubscribeMonitor: (() => void) | undefined;

  const ensureMonitor = () => {
    if (unsubscribeMonitor) {
      return;
    }

    online = eventBridge.requestSync({ type: 'org.racehorse.IsOnlineRequestEvent' })?.online;

    unsubscribeMonitor = eventBridge.subscribe(event => {
      if (event.type === 'org.racehorse.OnlineStatusChangedAlertEvent') {
        online = event.online;
        pubSub.publish();
      }
    });
  };

  const manager: NetworkManager = {
    online: undefined,

    subscribe(listener) {
      ensureMonitor();
      return pubSub.subscribe(listener);
    },
  };

  Object.defineProperty(manager, 'online', {
    get() {
      ensureMonitor();
      return online;
    },
  });

  return manager;
}
