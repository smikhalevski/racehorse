import { createContext, EffectCallback, useContext, useEffect, useReducer } from 'react';
import { ConnectionProvider, createConnectionProvider, createEventBridge, Event, EventBridge, Plugin } from 'racehorse';
import { useSemanticMemo } from 'react-hookers';

/**
 * Holds the callback that returns a {@linkcode Connection} object.
 *
 * See {@linkcode createConnectionProvider} for more info on connection providers.
 */
export const ConnectionProviderContext = createContext(createConnectionProvider(() => window.racehorseConnection));

ConnectionProviderContext.displayName = 'ConnectionProviderContext';

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 */
export function useEventBridge(): EventBridge;

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 *
 * @param plugin The plugin that enhances the event bridge with additional properties and methods.
 */
export function useEventBridge<M extends object>(plugin: Plugin<M>): EventBridge & M;

export function useEventBridge(plugin?: Plugin<object>) {
  const [, dispatch] = useReducer(reduceCount, 0);
  const connectionProvider = useContext(ConnectionProviderContext);

  const manager = useSemanticMemo(
    () => createEventBridgeManager(dispatch, plugin, connectionProvider),
    [connectionProvider]
  );

  useEffect(manager.effect, [manager]);

  return Object.assign({}, manager.eventBridge);
}

function createEventBridgeManager(
  rerender: () => void,
  plugin: Plugin<object> | undefined,
  connectionProvider: ConnectionProvider
) {
  let alertListeners: Array<(event: Event) => void> | undefined;
  let listeners: Array<() => void> | undefined;

  let originalSubscribeToAlerts: EventBridge['subscribeToAlerts'];
  let originalSubscribe: EventBridge['subscribe'];

  let internalSubscribeToAlerts: EventBridge['subscribeToAlerts'] = listener => {
    (alertListeners ||= []).push(listener);
    return noop;
  };

  let internalSubscribe: EventBridge['subscribe'] = listener => {
    (listeners ||= []).push(listener);
    return noop;
  };

  const eventBridge = createEventBridge((eventBridge, notify) => {
    originalSubscribeToAlerts = eventBridge.subscribeToAlerts;
    originalSubscribe = eventBridge.subscribe;

    eventBridge.subscribeToAlerts = listener => internalSubscribeToAlerts(listener);
    eventBridge.subscribe = listener => internalSubscribe(listener);

    plugin?.(eventBridge, notify);
  }, connectionProvider);

  const effect: EffectCallback = () => {
    let unsubscribes: Array<() => void> | undefined;

    internalSubscribeToAlerts = listener => {
      const unsubscribe = originalSubscribeToAlerts(listener);
      (unsubscribes ||= []).push(unsubscribe);
      return unsubscribe;
    };

    internalSubscribe = listener => {
      const unsubscribe = originalSubscribe(listener);
      (unsubscribes ||= []).push(unsubscribe);
      return unsubscribe;
    };

    alertListeners?.forEach(internalSubscribeToAlerts);

    listeners?.forEach(internalSubscribe);

    internalSubscribe(rerender);

    return () => {
      internalSubscribeToAlerts = internalSubscribe = () => noop;

      if (unsubscribes) {
        for (const unsubscribe of unsubscribes) {
          unsubscribe();
        }
        unsubscribes = undefined;
      }
    };
  };

  return {
    eventBridge,
    effect,
  };
}

function noop() {}

function reduceCount(count: number): number {
  return count + 1;
}
