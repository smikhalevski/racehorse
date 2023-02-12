import { PubSub } from 'parallel-universe';
import { Connection, Event, EventBridge, Plugin, ResponseEvent } from './shared-types';
import { createConnectionProvider } from './createConnectionProvider';
import { noop } from './utils';

const racehorseConnectionProvider = createConnectionProvider();

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 */
export function createEventBridge(
  plugin?: undefined,
  connectionProvider?: () => Connection | Promise<Connection>
): EventBridge;

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 *
 * @param plugin The plugin that enhances the event bridge with additional properties and methods.
 * @param connectionProvider The provider that returns the connection.
 */
export function createEventBridge<M extends object>(
  plugin: Plugin<M>,
  connectionProvider?: () => Connection | Promise<Connection>
): EventBridge & M;

export function createEventBridge(
  plugin?: Plugin<object>,
  connectionProvider = racehorseConnectionProvider
): EventBridge {
  const pubSub = new PubSub();

  const eventBridge: EventBridge = {
    waitForConnection() {
      return Promise.resolve(connectionProvider()).then(noop);
    },

    request(event) {
      const eventJson = JSON.stringify(event);

      return new Promise(resolve => {
        Promise.resolve(connectionProvider()).then(conn => {
          postToConnection(conn, eventJson, resolve);
        });
      });
    },

    requestSync(event) {
      const connection = connectionProvider();

      let responseEvent: ResponseEvent | undefined;

      if (!(connection instanceof Promise)) {
        postToConnection(connection, JSON.stringify(event), event => {
          responseEvent = event;
        });
      }
      return responseEvent;
    },

    watchForAlerts(listener) {
      const connection = connectionProvider();

      const alertListener = (envelope: [requestId: number, event: Event]) => {
        if (envelope[0] === -1) {
          listener(envelope[1]);
        }
      };

      if (!(connection instanceof Promise)) {
        prepareConnection(connection);
        return connection.inboxPubSub.subscribe(alertListener);
      }

      let unsubscribe: (() => void) | undefined;
      let unsubscribed = false;

      connection.then(conn => {
        if (!unsubscribed) {
          prepareConnection(conn);
          unsubscribe = conn.inboxPubSub.subscribe(alertListener);
        }
      });

      return () => {
        unsubscribed = true;
        unsubscribe?.();
      };
    },

    subscribe: pubSub.subscribe.bind(pubSub),
  };

  plugin?.(eventBridge, pubSub.publish.bind(pubSub));

  return eventBridge;
}

function prepareConnection(connection: Connection): asserts connection is Required<Connection> {
  connection.requestCount ||= 0;
  connection.inboxPubSub ||= new PubSub();
}

function postToConnection(connection: Connection, eventJson: string, callback: (event: ResponseEvent) => void): void {
  prepareConnection(connection);

  const requestId = connection.requestCount++;

  const unsubscribe = connection.inboxPubSub.subscribe(envelope => {
    if (envelope[0] === requestId) {
      unsubscribe();
      callback(envelope[1] as ResponseEvent);
    }
  });

  connection.post(requestId, eventJson);
}
