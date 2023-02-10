import { createContext, useContext, useEffect, useMemo, useReducer } from 'react';
import { createEventBridge, EventBridge, Plugin } from 'racehorse';

export const EventBridgeContext = createContext(createEventBridge());

EventBridgeContext.displayName = 'EventBridgeContext';

export function useEventBridge(): EventBridge;

export function useEventBridge<M>(plugin: Plugin<M>): EventBridge & M;

export function useEventBridge(plugin?: Plugin<unknown>) {
  const [, dispatch] = useReducer(reduceCount, 0);
  const eventBridge = useContext(EventBridgeContext);

  const unsubscribe = useMemo(() => plugin?.(Object.assign({}, eventBridge), dispatch), [eventBridge]);

  useEffect(() => unsubscribe, [eventBridge]);

  return Object.assign({}, eventBridge);
}

function reduceCount(count: number): number {
  return count + 1;
}
