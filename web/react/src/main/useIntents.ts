import { useEventBridge } from './useEventBridge';
import { useMemo } from 'react';
import { createIntentsManager } from 'racehorse';

/**
 * Launches activities for various intents.
 */
export function useIntents() {
  const eventBridge = useEventBridge();

  return useMemo(() => createIntentsManager(eventBridge), [eventBridge]);
}
