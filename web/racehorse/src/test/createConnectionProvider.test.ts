import { Connection, createConnectionProvider } from '../main';

describe('createConnectionProvider', () => {
  test('returns the available connection', () => {
    const connection: Connection = {
      post() {},
    };
    expect(createConnectionProvider(() => connection)()).toBe(connection);
  });

  test('returns the deferred connection', async () => {
    let connection: Connection | undefined;

    const connectionPromise = createConnectionProvider(() => connection)();

    expect(connectionPromise).toBeInstanceOf(Promise);

    connection = {
      post() {},
    };

    await expect(connectionPromise).resolves.toBe(connection);
  });

  test('returns the same promise even if connection is available before the promise resolves', async () => {
    let connection: Connection | undefined;

    const connectionProvider = createConnectionProvider(() => connection);

    const connectionPromise = connectionProvider();

    connection = {
      post() {},
    };

    expect(connectionProvider()).toBe(connectionPromise);

    await expect(connectionPromise).resolves.toBe(connection);
  });
});
