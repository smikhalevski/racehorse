import { useEventBridge } from './useEventBridge';
import { useMemo } from 'react';
import { createConfigurationManager } from 'racehorse';

/**
 * Provides access to application and device configuration.
 */
export function useConfiguration() {
  const eventBridge = useEventBridge();

  return useMemo(() => createConfigurationManager(eventBridge), [eventBridge]);
}
