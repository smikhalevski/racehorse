import { sleep } from 'parallel-universe';
import { Connection, createEventBridge } from '../main';

describe('EventBridge', () => {
  test('sends a request and gets a response when a connection is already initialized', async () => {
    const connection: Connection = {
      post: jest.fn(() => {
        connection.inbox?.push({
          requestId: 0,
          ok: true,
          event: { type: 'bbb', key1: 111 },
        });
      }),
    };

    const messageBus = createEventBridge({
      connectionProvider: () => connection,
    });

    await expect(messageBus.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', key1: 111 });

    expect(connection.post).toHaveBeenCalledTimes(1);
    expect(connection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('sends a request and gets a response when a connection is not yet initialized', async () => {
    const messageBus = createEventBridge({
      connectionProvider: () => connection,
    });

    const connection: Connection = {
      post: jest.fn(() => {
        connection?.inbox?.push({
          requestId: 0,
          ok: true,
          event: { type: 'bbb', key1: 111 },
        });
      }),
    };

    await expect(messageBus.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', key1: 111 });

    expect(connection.post).toHaveBeenCalledTimes(1);
    expect(connection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('rejects a request with an error', done => {
    const connection: Connection = {
      post() {
        connection.inbox?.push({
          requestId: 0,
          ok: false,
          event: { type: 'bbb', name: 'ccc', message: 'ddd', stack: 'eee' },
        });
      },
    };

    const messageBus = createEventBridge({
      connectionProvider: () => connection,
    });

    messageBus.request({ type: 'aaa' }).catch(error => {
      expect(error.name).toBe('ccc');
      expect(error.message).toBe('ddd');
      expect(error.stack).toBe('eee');
      done();
    });
  });

  test('listener receives a message', async () => {
    const listenerMock = jest.fn();

    const messageBus = createEventBridge({
      connectionProvider: () => connection,
    });

    messageBus.subscribe(listenerMock);

    const connection: Connection = {
      post() {},
    };

    await sleep(100);

    expect(connection.inbox).toBeDefined();

    connection.inbox?.push({
      requestId: -1,
      ok: true,
      event: { type: 'aaa', key1: 111 },
    });

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa', key1: 111 });
  });

  test('listener receives a message from the array inbox', async () => {
    const connection: Connection = {
      inbox: [
        {
          requestId: 0,
          ok: true,
          event: { type: 'aaa', key1: 111 },
        },
      ],
      post() {},
    };

    const listenerMock = jest.fn();

    const messageBus = createEventBridge({
      connectionProvider: () => connection,
    });

    messageBus.subscribe(listenerMock);

    await sleep(100);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa', key1: 111 });
  });

  test('handles an error', async () => {
    const connection: Connection = {
      inbox: [
        {
          requestId: 0,
          ok: false,
          event: { type: 'aaa', name: 'bbb', message: 'ccc', stack: 'ddd' },
        },
      ],
      post() {},
    };

    const listenerMock = jest.fn();
    const errorHandlerMock = jest.fn();

    const messageBus = createEventBridge({
      connectionProvider: () => connection,
      errorHandler: errorHandlerMock,
    });

    messageBus.subscribe(listenerMock);

    await sleep(100);

    expect(listenerMock).not.toHaveBeenCalled();
    expect(errorHandlerMock).toHaveBeenCalledTimes(1);

    const error = errorHandlerMock.mock.calls[0][0];

    expect(error).toBeInstanceOf(Error);
    expect(error.name).toBe('bbb');
    expect(error.message).toBe('ccc');
    expect(error.stack).toBe('ddd');
  });
});
