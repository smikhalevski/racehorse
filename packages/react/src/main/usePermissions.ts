import { useEventBridge } from './useEventBridge';
import { useMemo } from 'react';
import { createPermissionsManager } from 'racehorse';

/**
 * Allows checking and requesting application permissions.
 */
export function usePermissions() {
  const eventBridge = useEventBridge();

  return useMemo(() => createPermissionsManager(eventBridge), [eventBridge]);
}
