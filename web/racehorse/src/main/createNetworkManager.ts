import { PubSub } from 'parallel-universe';
import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface NetworkManager {
  /**
   * The current online status, or `undefined` if not yet known.
   */
  readonly isOnline: boolean | undefined;

  /**
   * Subscribes to network status changes.
   */
  subscribeToOnlineStatusChanges(listener: () => void): () => void;
}

/**
 * Monitors network status.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createNetworkManager(eventBridge: EventBridge): NetworkManager {
  const pubSub = new PubSub();

  let online: boolean | undefined;
  let unsubscribe: (() => void) | undefined;

  const ensureSubscription = () => {
    if (unsubscribe) {
      return;
    }

    eventBridge.request({ type: 'org.racehorse.IsOnlineRequestEvent' }).then(event => {
      online = ensureEvent(event).isOnline;
      pubSub.publish();
    });

    unsubscribe = eventBridge.subscribe(event => {
      if (event.type === 'org.racehorse.OnlineStatusChangedAlertEvent') {
        online = event.isOnline;
        pubSub.publish();
      }
    });
  };

  const manager: NetworkManager = {
    isOnline: undefined,

    subscribeToOnlineStatusChanges(listener) {
      ensureSubscription();
      return pubSub.subscribe(listener);
    },
  };

  Object.defineProperty(manager, 'isOnline', {
    get() {
      ensureSubscription();
      return online;
    },
  });

  return manager;
}
