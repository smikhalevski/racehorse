import { useEventBridge } from './useEventBridge';
import { useMemo } from 'react';
import { createNetworkManager } from 'racehorse';

export function useNetworkManager() {
  const eventBridge = useEventBridge();

  return useMemo(() => createNetworkManager(eventBridge), [eventBridge]);
}
