import { PubSub, untilTruthy } from 'parallel-universe';
import { Connection, Event, EventBridge, Plugin, ResponseEvent } from './shared-types';

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 */
export function createEventBridge(): EventBridge;

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 *
 * @param plugin The plugin that enhances the event bridge with additional properties and methods.
 */
export function createEventBridge<M extends object>(plugin: Plugin<M>): EventBridge & M;

export function createEventBridge(plugin?: Plugin<object>): EventBridge {
  const bridgeChannel = new PubSub();

  let activeConnection = getConnection();

  const connectionPromise = untilTruthy(getConnection, 200).then(connection => (activeConnection = connection));

  const eventBridge: EventBridge = {
    request(event) {
      const json = JSON.stringify(event);

      return new Promise(resolve => {
        connectionPromise.then(connection => {
          post(connection, json, resolve);
        });
      });
    },

    requestSync(event) {
      let responseEvent: ResponseEvent | undefined;
      if (activeConnection) {
        post(activeConnection, JSON.stringify(event), event => {
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

      if (activeConnection) {
        return activeConnection.inboxChannel.subscribe(alertListener);
      }

      let unsubscribe: (() => void) | undefined;
      let unsubscribed = false;

      connectionPromise.then(connection => {
        if (!unsubscribed) {
          unsubscribe = connection.inboxChannel.subscribe(alertListener);
        }
      });

      return () => {
        unsubscribed = true;
        unsubscribe?.();
      };
    },

    subscribeToBridge: bridgeChannel.subscribe.bind(bridgeChannel),
  };

  plugin?.(eventBridge, bridgeChannel.publish.bind(bridgeChannel));

  return eventBridge;
}

function getConnection(): Required<Connection> | undefined {
  const connection = window.racehorseConnection;
  if (connection) {
    connection.requestCount ||= 0;
    connection.inboxChannel ||= new PubSub();
  }
  return connection as Required<Connection> | undefined;
}

function post(connection: Required<Connection>, json: string, callback: (event: ResponseEvent) => void): void {
  const requestId = connection.requestCount++;

  const unsubscribe = connection.inboxChannel.subscribe(envelope => {
    if (envelope[0] === requestId) {
      unsubscribe();
      callback(envelope[1] as ResponseEvent);
    }
  });

  connection.post(requestId, json);
}
