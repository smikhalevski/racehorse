import { EventBridge } from './createEventBridge.js';
import { Unsubscribe } from './types.js';

export const NetworkType = {
  WIFI: 'wifi',
  CELLULAR: 'cellular',
  NONE: 'none',
  UNKNOWN: 'unknown',
} as const;

export type NetworkType = (typeof NetworkType)[keyof typeof NetworkType];

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
  subscribe(listener: (status: NetworkStatus) => void): Unsubscribe;
}

/**
 * Monitors the network status.
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
