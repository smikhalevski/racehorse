import { createContext, useContext, useEffect, useState } from 'react';
import { networkManager, NetworkStatus } from 'racehorse';

/**
 * Provides the `NetworkManager` instance to underlying components.
 */
export const NetworkManagerContext = createContext(networkManager);

NetworkManagerContext.displayName = 'NetworkManagerContext';

/**
 * Returns the current network status and re-renders the component if it changes.
 */
export function useNetworkStatus(): NetworkStatus {
  const manager = useContext(NetworkManagerContext);

  const [status, setStatus] = useState(manager.getStatus);

  useEffect(() => manager.subscribe(setStatus), [manager]);

  return status;
}
