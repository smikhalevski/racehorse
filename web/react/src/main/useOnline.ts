import { createContext, useContext, useEffect, useState } from 'react';
import { networkManager } from 'racehorse';

/**
 * Provides the `NetworkManager` instance to underlying components.
 */
export const NetworkManagerContext = createContext(networkManager);

NetworkManagerContext.displayName = 'NetworkManagerContext';

/**
 * Returns the current online status, or `undefined` if not yet known. Re-renders the component if the online status is
 * changed.
 */
export function useOnline(): boolean | undefined {
  const manager = useContext(NetworkManagerContext);

  const [online, setOnline] = useState<boolean>();

  useEffect(() => manager.subscribe(() => setOnline(manager.isOnline)), [manager]);

  return online;
}
