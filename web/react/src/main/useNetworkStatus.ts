import { createContext, useContext, useEffect, useState } from 'react';
import { networkManager, NetworkStatus } from 'racehorse';

/**
 * Provides the `NetworkManager` instance to underlying components.
 */
export const NetworkManagerContext = createContext(networkManager);

NetworkManagerContext.displayName = 'NetworkManagerContext';

/**
 * Returns the current online status, or `undefined` if not yet known. Re-renders the component if the online status is
 * changed.
 */
export function useNetworkStatus(): Partial<NetworkStatus> {
  const manager = useContext(NetworkManagerContext);

  const [status, setStatus] = useState<Partial<NetworkStatus>>({});

  useEffect(() => {
    manager.getStatus().then(setStatus);

    return manager.subscribe(setStatus);
  }, [manager]);

  return status;
}
