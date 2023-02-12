import { PubSub, repeatUntil } from 'parallel-universe';
import { Connection, ConnectionProvider } from './shared-types';
import { noop } from './utils';

/**
 * Creates a connection provider.
 *
 * @param getConnection Returns a {@linkcode Connection} object or `undefined` if there's connection.
 * @param pollDelay The delay between connection availability checks.
 * @param noopDelay The delay after which the noop connection is returned.
 */
export function createConnectionProvider(
  getConnection = () => window.racehorseConnection,
  pollDelay = 100,
  noopDelay = 60_000
): ConnectionProvider {
  let connection: Connection | undefined;
  let connectionPromise: Promise<Connection> | undefined;
  let timestamp: number;

  return () => {
    timestamp ||= Date.now();

    return (
      connectionPromise ||
      (connection ||= getConnection()) ||
      (connectionPromise ||= repeatUntil(
        getConnection,
        result => result.result || Date.now() - timestamp > noopDelay,
        pollDelay
      ).then(conn => {
        connection ||= conn || noopConnection;
        connectionPromise = undefined;
        return connection;
      }))
    );
  };
}

const noopConnection: Connection = {
  requestCount: 0,
  inboxPubSub: new PubSub(),
  post: noop,
};

noopConnection.inboxPubSub!.subscribe = () => noop;
