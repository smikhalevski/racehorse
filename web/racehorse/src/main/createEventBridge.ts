import { PubSub, untilTruthy } from 'parallel-universe';
import { Connection, Event, EventBridge, ResponseEvent } from './types';

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
  const alertsPubSub = new PubSub<Event>();

  let connectionPromise: Promise<Required<Connection>> | undefined;
  let tryCount = 0;

  const connect = (): Promise<Required<Connection>> =>
    (connectionPromise ||= untilTruthy(connectionProvider, () => 2 ** tryCount++ * 100).then(connection => {
      (connection.inbox ||= new PubSub()).subscribe(envelope => {
        if (envelope[0] === -1) {
          alertsPubSub.publish(envelope[1]);
        }
      });

      return connection as Required<Connection>;
    }));

  return {
    connect,

    request(event) {
      const eventData = JSON.stringify(event);

      return connect().then(
        connection =>
          new Promise(resolve => {
            const requestId = connection.post(eventData);

            const unsubscribe = connection.inbox.subscribe(envelope => {
              if (envelope[0] === requestId) {
                unsubscribe();
                resolve(envelope[1] as ResponseEvent);
              }
            });
          })
      );
    },

    subscribe(listener) {
      void connect();
      return alertsPubSub.subscribe(listener);
    },
  };
}
