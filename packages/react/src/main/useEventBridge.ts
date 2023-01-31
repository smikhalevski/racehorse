import { createContext, useContext, useEffect } from 'react';
import { createEventBridge, Event, EventBridge } from 'racehorse';

let eventBridge: EventBridge | undefined;

export const EventBridgeProviderContext = createContext(() => (eventBridge ||= createEventBridge()));

export function useEventBridge(): EventBridge {
  return useContext(EventBridgeProviderContext)();
}

export function useEventBridgeSubscription(listener: (event: Event) => void): void {
  const eventBridge = useEventBridge();

  useEffect(() => eventBridge.subscribe(listener), [eventBridge]);
}
