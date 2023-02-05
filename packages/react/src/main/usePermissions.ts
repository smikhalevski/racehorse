import { useEventBridge } from './useEventBridge';
import { useMemo } from 'react';
import { createPermissionManager } from 'racehorse';

/**
 * Allows checking and requesting application permissions.
 */
export function usePermissions() {
  const eventBridge = useEventBridge();

  return useMemo(() => createPermissionManager(eventBridge), [eventBridge]);
}
