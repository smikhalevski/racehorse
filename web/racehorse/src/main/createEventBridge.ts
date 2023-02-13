import { PubSub } from 'parallel-universe';
import { Connection, ConnectionProvider, Event, EventBridge, Plugin, ResponseEvent } from './shared-types';
import { createConnectionProvider } from './createConnectionProvider';

const racehorseConnectionProvider = createConnectionProvider(() => window.racehorseConnection);

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 */
export function createEventBridge(): EventBridge;

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 *
 * @param plugin The plugin that enhances the event bridge with additional properties and methods.
 * @param connectionProvider The provider that returns the connection. Consider using
 * {@linkcode createConnectionProvider}. By default, the provider of `window.racehorseConnection` is used.
 */
export function createEventBridge<M extends object>(
  plugin: Plugin<M>,
  connectionProvider?: ConnectionProvider
): EventBridge & M;

export function createEventBridge(
  plugin?: Plugin<object>,
  connectionProvider = racehorseConnectionProvider
): EventBridge {
  const pubSub = new PubSub();

  let alertUnsubscribeCallbacks: Map<(event: Event) => void, () => void> | undefined;

  let connection: Required<Connection> | undefined;
  let connectionPromise: Promise<void> | undefined;

  /**
   * Ensures that the connection is established or being established.
   */
  const ensureConnection = () => {
    if (connection || connectionPromise) {
      return;
    }

    const conn = connectionProvider();

    if (conn instanceof Promise) {
      connectionPromise = conn.then(conn => {
        prepareConnection(conn);
        connection = conn;
      });
    } else {
      prepareConnection(conn);
      connection = conn;
      connectionPromise = Promise.resolve();
    }
  };

  const eventBridge: EventBridge = {
    waitForConnection() {
      ensureConnection();
      return connectionPromise!;
    },

    request(event) {
      ensureConnection();

      return new Promise(resolve => {
        const eventJson = JSON.stringify(event);

        connectionPromise!.then(() => {
          postToConnection(connection!, eventJson, resolve);
        });
      });
    },

    requestSync(event) {
      ensureConnection();

      let responseEvent: ResponseEvent | undefined;
      if (connection) {
        postToConnection(connection, JSON.stringify(event), event => {
          responseEvent = event;
        });
      }
      return responseEvent;
    },

    subscribeToAlerts(listener) {
      let unsub = alertUnsubscribeCallbacks?.get(listener);

      if (unsub) {
        return unsub;
      }

      ensureConnection();

      const alertListener = (envelope: [requestId: number, event: Event]) => {
        if (envelope[0] === -1) {
          listener(envelope[1]);
        }
      };

      if (connection) {
        const unsubscribe = connection.inboxPubSub.subscribe(alertListener);
        unsub = () => {
          alertUnsubscribeCallbacks!.delete(listener);
          unsubscribe();
        };
      } else {
        let unsubscribe: (() => void) | undefined;
        let unsubscribed = false;

        connectionPromise!.then(() => {
          if (!unsubscribed) {
            unsubscribe = connection!.inboxPubSub.subscribe(alertListener);
          }
        });

        unsub = () => {
          alertUnsubscribeCallbacks!.delete(listener);
          unsubscribed = true;
          unsubscribe?.();
        };
      }

      (alertUnsubscribeCallbacks ||= new Map())?.set(listener, unsub);

      return unsub;
    },

    subscribe: pubSub.subscribe.bind(pubSub),
  };

  plugin?.(eventBridge)?.(pubSub.publish.bind(pubSub));

  return eventBridge;
}

function prepareConnection(connection: Connection): asserts connection is Required<Connection> {
  connection.requestCount ||= 0;
  connection.inboxPubSub ||= new PubSub();
}

function postToConnection(
  connection: Required<Connection>,
  eventJson: string,
  callback: (event: ResponseEvent) => void
): void {
  const requestId = connection.requestCount++;

  const unsubscribe = connection.inboxPubSub.subscribe(envelope => {
    if (envelope[0] === requestId) {
      unsubscribe();
      callback(envelope[1] as ResponseEvent);
    }
  });

  connection.post(requestId, eventJson);
}
