import { useEventBridge } from './useEventBridge';
import { useMemo } from 'react';
import { createNetworkManager } from 'racehorse';

/**
 * Device network monitoring.
 */
export function useNetwork() {
  const eventBridge = useEventBridge();

  return useMemo(() => createNetworkManager(eventBridge), [eventBridge]);
}
