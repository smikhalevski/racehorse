import { sleep } from 'parallel-universe';
import { createConnectionProvider } from '../main/createConnectionProvider';

describe('createConnectionProvider', () => {
  beforeEach(() => {
    delete window.racehorseConnection;
  });

  test('returns the available connection', () => {
    const connection = (window.racehorseConnection = {
      post() {},
    });
    expect(createConnectionProvider()()).toBe(connection);
  });

  test('returns the deferred connection', async () => {
    const promise = createConnectionProvider()();

    expect(promise).toBeInstanceOf(Promise);

    const connection = (window.racehorseConnection = {
      post() {},
    });

    await expect(promise).resolves.toBe(connection);
  });

  test('returns the noop connection after noop delay elapses', async () => {
    const promise = createConnectionProvider(1, 10)();

    await sleep(20);

    const connection = await promise;

    const listenerMock = jest.fn();

    connection.inboxChannel!.subscribe(listenerMock);
    connection.inboxChannel!.publish([0, { type: 'aaa' }]);

    expect(listenerMock).not.toHaveBeenCalled();
  });
});
