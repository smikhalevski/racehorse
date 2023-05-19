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
  getStatus(): NetworkStatus;

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
      ensureEvent(eventBridge.requestSync({ type: 'org.racehorse.GetNetworkStatusEvent' })).payload.status,

    subscribe: listener =>
      eventBridge.subscribe(event => {
        if (event.type === 'org.racehorse.NetworkStatusChangedEvent') {
          listener(event.payload.status);
        }
      }),
  };
}
