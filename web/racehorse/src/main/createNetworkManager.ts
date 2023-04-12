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
  return {
    getStatus: () =>
      eventBridge
        .request({ type: 'org.racehorse.GetNetworkStatusRequestEvent' })
        .then(event => ensureEvent(event).status),

    subscribe: listener =>
      eventBridge.subscribe(event => {
        if (event.type === 'org.racehorse.NetworkStatusChangedEvent') {
          listener(event.status);
        }
      }),
  };
}
