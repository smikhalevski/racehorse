import { PubSub } from 'parallel-universe';
import { EventBridge } from './types';
import { ensureEvent } from './utils';

export type NetworkType = 'wifi' | 'cellular' | 'none' | 'unknown';

export interface NetworkStatus {
  type: NetworkType;
  isConnected: boolean;
}

export interface NetworkManager {
  /**
   * Returns the status of the active network.
   */
  getStatus(): Promise<NetworkStatus>;

  /**
   * Subscribes to network status changes.
   */
  subscribe(listener: (status: NetworkStatus) => void): () => void;
}

/**
 * Monitors network status.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createNetworkManager(eventBridge: EventBridge): NetworkManager {
  let pubSub: PubSub<NetworkStatus>;

  return {
    getStatus() {
      return eventBridge
        .request({ type: 'org.racehorse.GetNetworkStatusRequestEvent' })
        .then(event => ensureEvent(event).status);
    },

    subscribe(listener) {
      pubSub ||=
        (eventBridge.subscribe(event => {
          if (event.type === 'org.racehorse.NetworkStatusChangedEvent') {
            pubSub.publish(event.status);
          }
        }),
        new PubSub());

      return pubSub.subscribe(listener);
    },
  };
}
