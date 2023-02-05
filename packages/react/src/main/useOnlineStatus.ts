import { useNetworkManager } from './useNetworkManager';
import { useEffect, useState } from 'react';

/**
 * Returns `true` if device is online, `false` if device is offline, and `undefined` if status isn't yet known. The
 * component is re-rendered when the status is changed.
 */
export function useOnlineStatus(): boolean | undefined {
  const [online, setOnline] = useState<boolean>();
  const networkManager = useNetworkManager();

  useEffect(() => networkManager.subscribe(setOnline), [networkManager]);

  return online;
}
