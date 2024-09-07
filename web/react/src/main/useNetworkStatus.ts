import { useEffect, useState } from 'react';
import { NetworkStatus } from 'racehorse';
import { useNetworkManager } from './managers';

/**
 * Returns the current network status and re-renders the component if it changes.
 */
export function useNetworkStatus(): NetworkStatus {
  const manager = useNetworkManager();

  const [networkStatus, setNetworkStatus] = useState(manager.getNetworkStatus);

  useEffect(() => manager.subscribe(setNetworkStatus), [manager]);

  return networkStatus;
}
