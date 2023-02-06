import { useNetwork } from './useNetwork';
import { useEffect, useState } from 'react';

/**
 * Returns `true` if device is online, `false` if device is offline, and `undefined` if status isn't yet known.
 *
 * The component re-renders if online status changes.
 */
export function useOnline(): boolean | undefined {
  const [online, setOnline] = useState<boolean>();
  const networkManager = useNetwork();

  useEffect(() => networkManager.subscribe(setOnline), [networkManager]);

  return online;
}
