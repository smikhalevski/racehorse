import { useEventBridge } from './useEventBridge';
import { useMemo } from 'react';
import { createPermissionManager } from 'racehorse';

export function usePermissionManager() {
  const eventBridge = useEventBridge();

  return useMemo(() => createPermissionManager(eventBridge), [eventBridge]);
}
