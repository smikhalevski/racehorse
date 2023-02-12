import { PubSub, untilTruthy } from 'parallel-universe';
import { Connection, Event, EventBridge, Plugin, ResponseEvent } from './shared-types';

interface ConnectionProvider {
  getConnection(): Connection | undefined;

  getConnectionPromise(): Promise<Connection>;
}

const getConnection = () => window.racehorseConnection;

const racehorseConnectionProvider: ConnectionProvider = {
  getConnection() {
    return window.racehorseConnection;
  },
  getConnectionPromise() {
    const promise = untilTruthy(getConnection, 200);
    racehorseConnectionProvider.getConnectionPromise = () => promise;
    return promise;
  },
};

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 */
export function createEventBridge(connectionProvider?: ConnectionProvider): EventBridge;

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 *
 * @param plugin The plugin that enhances the event bridge with additional properties and methods.
 * @param connectionProvider The provider that returns the connection.
 */
export function createEventBridge<M extends object>(
  plugin: Plugin<M>,
  connectionProvider?: ConnectionProvider
): EventBridge & M;

export function createEventBridge(
  plugin?: Plugin<object> | ConnectionProvider,
  connectionProvider = racehorseConnectionProvider
): EventBridge {
  if (typeof plugin === 'object') {
    connectionProvider = plugin;
  }

  const bridgeChannel = new PubSub();

  let connection = connectionProvider.getConnection();
  let getConnectionPromise = connectionProvider.getConnectionPromise;

  if (connection) {
    prepareConnection(connection);
  }

  const eventBridge: EventBridge = {
    request(event) {
      const json = JSON.stringify(event);

      return new Promise(resolve => {
        getConnectionPromise().then(connection => {
          post(connection, json, resolve);
        });
      });
    },

    requestSync(event) {
      let responseEvent: ResponseEvent | undefined;

      if (connection) {
        post(connection, JSON.stringify(event), event => {
          responseEvent = event;
        });
      }
      return responseEvent;
    },

    subscribeToAlerts(listener) {
      const alertListener = (envelope: [requestId: number, event: Event]) => {
        if (envelope[0] === -1) {
          listener(envelope[1]);
        }
      };

      if (connection) {
        return connection.inboxChannel!.subscribe(alertListener);
      }

      let unsubscribe: (() => void) | undefined;
      let unsubscribed = false;

      getConnectionPromise().then(connection => {
        if (!unsubscribed) {
          unsubscribe = connection.inboxChannel!.subscribe(alertListener);
        }
      });

      return () => {
        unsubscribed = true;
        unsubscribe?.();
      };
    },

    subscribeToBridge: bridgeChannel.subscribe.bind(bridgeChannel),
  };

  if (typeof plugin === 'function') {
    plugin(eventBridge, bridgeChannel.publish.bind(bridgeChannel));
  }

  return eventBridge;
}

// const noopConnection: Connection = {
//   requestCount: 0,
//   inboxChannel: new PubSub(),
//   post() {},
// };
//
// function noop() {}
//
// noopConnection.inboxChannel!.subscribe = () => noop;
//
// function getConnection(): Connection | undefined {
//   return window.racehorseConnection;
// }

function prepareConnection(connection: Connection): asserts connection is Required<Connection> {
  connection.requestCount ||= 0;
  connection.inboxChannel ||= new PubSub();
}

function post(connection: Connection, json: string, callback: (event: ResponseEvent) => void): void {
  prepareConnection(connection);

  const requestId = connection.requestCount++;

  const unsubscribe = connection.inboxChannel.subscribe(envelope => {
    if (envelope[0] === requestId) {
      unsubscribe();
      callback(envelope[1] as ResponseEvent);
    }
  });

  connection.post(requestId, json);
}
