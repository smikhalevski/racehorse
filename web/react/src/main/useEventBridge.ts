import { createContext, useContext } from 'react';
import { createEventBridge, EventBridge } from 'racehorse';

export const EventBridgeContext = createContext(createEventBridge());

EventBridgeContext.displayName = 'EventBridgeContext';

export function useEventBridge(): EventBridge {
  return useContext(EventBridgeContext);
}
