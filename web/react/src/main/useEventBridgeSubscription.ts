import { Event } from 'racehorse';
import { useEffect } from 'react';
import { useEventBridge } from './useEventBridge';

export function useEventBridgeSubscription(listener: (event: Event) => void): void {
  const eventBridge = useEventBridge();

  useEffect(() => eventBridge.subscribe(listener), [eventBridge]);
}
