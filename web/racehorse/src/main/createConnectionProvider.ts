import { PubSub, repeatUntil } from 'parallel-universe';
import { Connection, ConnectionProvider } from './shared-types';

/**
 * Creates a connection provider.
 *
 * @param pollDelay The delay between connection availability checks.
 * @param noopDelay The delay after which the noop connection is returned.
 */
export function createConnectionProvider(pollDelay = 200, noopDelay = 60_000): ConnectionProvider {
  let connection: Connection | undefined;
  let connectionPromise: Promise<Connection> | undefined;
  let timestamp: number;

  return () => {
    timestamp ||= Date.now();

    return (
      (connection ||= window.racehorseConnection) ||
      (connectionPromise ||= repeatUntil(
        () => window.racehorseConnection,
        result => result.result || Date.now() - timestamp > noopDelay,
        pollDelay
      ).then(conn => (connection ||= conn || noopConnection)))
    );
  };
}

function noop() {}

const noopConnection: Connection = {
  requestCount: 0,
  inboxChannel: new PubSub(),
  post: noop,
};

noopConnection.inboxChannel!.subscribe = () => noop;
