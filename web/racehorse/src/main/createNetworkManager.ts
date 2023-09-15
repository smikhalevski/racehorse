import { EventBridge } from './createEventBridge';

export type NetworkType = 'wifi' | 'cellular' | 'none' | 'unknown';

export interface NetworkStatus {
  type: NetworkType;
  isConnected: boolean;
}

export interface NetworkManager {
  /**
   * Returns the status of the active network.
   */
  getNetworkStatus(): NetworkStatus;

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
    getNetworkStatus: () => eventBridge.request({ type: 'org.racehorse.GetNetworkStatusEvent' }).payload.status,

    subscribe: listener =>
      eventBridge.subscribe('org.racehorse.NetworkStatusChangedEvent', payload => {
        listener(payload.status);
      }),
  };
}
