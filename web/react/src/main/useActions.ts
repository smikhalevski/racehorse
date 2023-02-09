import { useEventBridge } from './useEventBridge';
import { useMemo } from 'react';
import { createActionsManager } from 'racehorse';

/**
 * Launches activities for various intents.
 */
export function useActions() {
  const eventBridge = useEventBridge();

  return useMemo(() => createActionsManager(eventBridge), [eventBridge]);
}
