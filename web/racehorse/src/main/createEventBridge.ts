import { PubSub } from 'parallel-universe';
import { Connection, Event, EventBridge, ResponseEvent } from './types';
import { createConnectionProvider } from './createConnectionProvider';

interface Window {
  /**
   * The connection object injected by Android.
   */
  racehorseConnection?: Connection;
}

const racehorseConnectionProvider = createConnectionProvider(() => (window as any).racehorseConnection);

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 *
 * @param connectionProvider The provider that returns the connection. Consider using
 * {@linkcode createConnectionProvider}. By default, the provider of `window.racehorseConnection` is used.
 */
export function createEventBridge(connectionProvider = racehorseConnectionProvider): EventBridge {
  const alertPubSub = new PubSub<Event>();

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
        prepareConnection(conn, alertPubSub);

        connection = conn;
      });
    } else {
      prepareConnection(conn, alertPubSub);

      connection = conn;
      connectionPromise = Promise.resolve();
    }
  };

  return {
    connect() {
      ensureConnection();
      return connectionPromise!;
    },

    request(event) {
      ensureConnection();

      return new Promise(resolve => {
        const eventData = JSON.stringify(event);

        connectionPromise!.then(() => {
          postToConnection(connection!, eventData, resolve);
        });
      });
    },

    requestSync(event) {
      ensureConnection();

      let responseEvent: ResponseEvent | undefined;
      if (connection) {
        postToConnection(connection, JSON.stringify(event), event => {
          responseEvent = event;
        })();
      }
      return responseEvent;
    },

    subscribe(listener) {
      ensureConnection();
      return alertPubSub.subscribe(listener);
    },
  };
}

function prepareConnection(
  connection: Connection,
  alertPubSub: PubSub<Event>
): asserts connection is Required<Connection> {
  connection.requestCount ||= 0;
  connection.inboxPubSub ||= new PubSub();

  connection.inboxPubSub.subscribe(envelope => {
    if (envelope[0] === -1) {
      alertPubSub.publish(envelope[1]);
    }
  });
}

function postToConnection(
  connection: Required<Connection>,
  eventData: string,
  callback: (event: ResponseEvent) => void
): () => void {
  const requestId = connection.requestCount++;

  const unsubscribe = connection.inboxPubSub.subscribe(envelope => {
    if (envelope[0] === requestId) {
      unsubscribe();
      callback(envelope[1] as ResponseEvent);
    }
  });

  connection.post(requestId, eventData);

  return unsubscribe;
}
