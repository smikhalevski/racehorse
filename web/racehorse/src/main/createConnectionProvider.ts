import { untilTruthy } from 'parallel-universe';
import { Connection, ConnectionProvider } from './shared-types';

/**
 * Creates a callback that invokes `getConnection` until a {@linkcode Connection} object is returned. Exponentially
 * increases delay between `getConnection` calls if `undefined` is returned.
 *
 * The returned connection provides is guaranteed to return the same promise so the connection is resolved
 * simultaneously for all connection consumers.
 *
 * @param getConnection Returns a {@linkcode Connection} object or `undefined` if there's no connection available.
 */
export function createConnectionProvider(getConnection: () => Connection | undefined): ConnectionProvider {
  let connection: Connection | undefined;
  let connectionPromise: Promise<Connection> | undefined;
  let tryCount = 0;

  return () =>
    connectionPromise ||
    (connection ||= getConnection()) ||
    (connectionPromise ||= untilTruthy(getConnection, () => 2 ** tryCount++ * 100).then(conn => {
      connection = conn;
      connectionPromise = undefined;
      return connection;
    }));
}
