import { PubSub, untilTruthy } from 'parallel-universe';
import { Connection, Event, EventBridge } from './types';

declare global {
  interface Window {
    /**
     * The connection object injected by Android.
     */
    racehorseConnection?: Connection;
  }
}

/**
 * Creates an event bridge that transports events from and to Android through the connection.
 *
 * Exponentially increases delay between `connectionProvider` calls if `undefined` is returned.
 *
 * @param connectionProvider The provider that returns the connection. By default, the provider of
 * `window.racehorseConnection` is used.
 */
export function createEventBridge(connectionProvider = () => window.racehorseConnection): EventBridge {
  const noticePubSub = new PubSub<Event>();

  let connectionPromise: Promise<Required<Connection>> | undefined;
  let connection: Required<Connection> | null = null;
  let tryCount = 0;

  const connect = (): Promise<Required<Connection>> =>
    (connectionPromise ||= untilTruthy(connectionProvider, () => 2 ** tryCount++ * 100).then(conn => {
      (conn.inbox ||= new PubSub()).subscribe(envelope => {
        if (envelope[0] === -1) {
          noticePubSub.publish(envelope[1]);
        }
      });
      return (connection = conn as Required<Connection>);
    }));

  return {
    connect,

    request(event) {
      const eventJson = JSON.stringify(event);

      return connect().then(
        connection =>
          new Promise(resolve => {
            const result = JSON.parse(connection.post(eventJson));

            if (typeof result === 'object') {
              resolve(result);
              return;
            }

            const unsubscribe = connection.inbox.subscribe(envelope => {
              if (envelope[0] === result) {
                unsubscribe();
                resolve(envelope[1]);
              }
            });
          })
      );
    },

    requestSync(event) {
      const result = connection !== null ? JSON.parse(connection.post(JSON.stringify(event))) : -1;

      return typeof result === 'object' ? result : null;
    },

    subscribe(listener) {
      void connect();
      return noticePubSub.subscribe(listener);
    },
  };
}
