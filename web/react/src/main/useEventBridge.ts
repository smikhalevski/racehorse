import { createContext, useContext, useEffect, useMemo, useReducer } from 'react';
import { createConnectionProvider, createEventBridge, EventBridge, Plugin } from 'racehorse';

export const ConnectionProviderContext = createContext(createConnectionProvider());

ConnectionProviderContext.displayName = 'ConnectionProviderContext';

export function useEventBridge(): EventBridge;

export function useEventBridge<M extends object>(plugin: Plugin<M>): EventBridge & M;

export function useEventBridge(plugin?: Plugin<object>) {
  const [, dispatch] = useReducer(reduceCount, 0);
  const connectionProvider = useContext(ConnectionProviderContext);

  const eventBridge = useMemo(() => createEventBridge(plugin!, connectionProvider), [connectionProvider]);

  useEffect(() => eventBridge.subscribe(dispatch), [eventBridge]);

  return Object.assign({}, eventBridge);
}

function reduceCount(count: number): number {
  return count + 1;
}
