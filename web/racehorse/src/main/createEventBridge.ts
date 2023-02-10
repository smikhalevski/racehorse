import { PubSub, untilTruthy } from 'parallel-universe';
import { Connection, Event, EventBridge, Plugin, ResponseEvent } from './shared-types';
import { noop } from './utils';

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 *
 * @param connectionProvider returns the connection opened by Android. The callback is polled until the connection is
 * returned. By default, {@linkcode window.racehorseConnection} is awaited.
 */
export function createEventBridge(connectionProvider?: () => Connection | undefined): EventBridge;

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 *
 * @param connectionProvider returns the connection opened by Android. The callback is polled until the connection is
 * returned. By default, {@linkcode window.racehorseConnection} is awaited.
 * @param plugin The plugin that is enhances the returned event bus.
 * @param listener The listener that is passed to the plugin, so it can notify external consumer about event bus changes.
 */
export function createEventBridge<M>(
  connectionProvider: (() => Connection | undefined) | undefined,
  plugin: Plugin<M>,
  listener?: () => void
): EventBridge & M;

export function createEventBridge(
  connectionProvider = () => window.racehorseConnection,
  plugin?: Plugin<unknown>,
  listener = noop
): EventBridge {
  if (plugin) {
    const eventBus = createEventBridge(connectionProvider);
    plugin(eventBus, listener);
    return eventBus;
  }

  const pubSub = new PubSub<Event>();
  const resolvers = new Map<number, (event: ResponseEvent) => void>();

  const inbox: Connection['inbox'] = {
    push([requestId, event]) {
      if (requestId === null) {
        pubSub.publish(event);
        return;
      }

      const resolver = resolvers.get(requestId);

      if (resolver) {
        resolvers.delete(requestId);
        resolver(event as ResponseEvent);
      }
    },
  };

  let requestCount = 0;

  let openConnection = (): Promise<Connection> => {
    const connectionPromise = untilTruthy(connectionProvider, 100).then(connection => {
      const responses = connection.inbox;

      connection.inbox = inbox;

      if (Array.isArray(responses)) {
        for (const response of responses) {
          inbox.push(response);
        }
      } else if (responses) {
        throw new Error('Another event bridge is connected');
      }

      return connection;
    });

    openConnection = () => connectionPromise;

    return connectionPromise;
  };

  return {
    request(event) {
      return new Promise(resolve => {
        const requestId = requestCount++;
        const eventJson = JSON.stringify(event);

        resolvers.set(requestId, resolve);

        openConnection().then(connection => {
          connection.post(requestId, eventJson);
        });
      });
    },

    subscribeToAlerts(listener) {
      void openConnection();
      return pubSub.subscribe(listener);
    },
  };
}
