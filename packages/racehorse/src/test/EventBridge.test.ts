import { sleep } from 'parallel-universe';
import { Connection, createEventBridge } from '../main';

describe('EventBridge', () => {
  test('updates connection status', async () => {
    const eventBridge = createEventBridge({
      connectionProvider: () => connection,
    });

    expect(eventBridge.connected).toBe(false);

    const connection: Connection = {
      post() {},
    };

    await sleep(200);

    expect(eventBridge.connected).toBe(true);
  });

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

    const eventBridge = createEventBridge({
      connectionProvider: () => connection,
    });

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', key1: 111 });

    expect(connection.post).toHaveBeenCalledTimes(1);
    expect(connection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('sends a request and gets a response when a connection is not yet initialized', async () => {
    const eventBridge = createEventBridge({
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

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', key1: 111 });

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

    const eventBridge = createEventBridge({
      connectionProvider: () => connection,
    });

    eventBridge.request({ type: 'aaa' }).catch(error => {
      expect(error.name).toBe('ccc');
      expect(error.message).toBe('ddd');
      expect(error.stack).toBe('eee');
      done();
    });
  });

  test('listener receives a message', async () => {
    const listenerMock = jest.fn();

    const eventBridge = createEventBridge({
      connectionProvider: () => connection,
    });

    eventBridge.subscribe(listenerMock);

    const connection: Connection = {
      post() {},
    };

    await sleep(200);

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
          requestId: -1,
          ok: true,
          event: { type: 'aaa', key1: 111 },
        },
      ],
      post() {},
    };

    const listenerMock = jest.fn();

    createEventBridge({ connectionProvider: () => connection }).subscribe(listenerMock);

    await sleep(200);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa', key1: 111 });
  });

  test('listener does not receive a message with invalid request ID', async () => {
    const connection: Connection = {
      inbox: [
        {
          requestId: -555,
          ok: true,
          event: { type: 'aaa', key1: 111 },
        },
      ],
      post() {},
    };

    const listenerMock = jest.fn();

    createEventBridge({ connectionProvider: () => connection }).subscribe(listenerMock);

    await sleep(200);

    expect(listenerMock).not.toHaveBeenCalled();
  });

  test('passes an error to error handler', async () => {
    const connection: Connection = {
      inbox: [
        {
          requestId: -1,
          ok: false,
          event: { type: 'aaa', name: 'bbb', message: 'ccc', stack: 'ddd' },
        },
      ],
      post() {},
    };

    const listenerMock = jest.fn();
    const errorHandlerMock = jest.fn();

    const eventBridge = createEventBridge({
      connectionProvider: () => connection,
      errorHandler: errorHandlerMock,
    });

    eventBridge.subscribe(listenerMock);

    await sleep(200);

    expect(listenerMock).not.toHaveBeenCalled();
    expect(errorHandlerMock).toHaveBeenCalledTimes(1);

    const error = errorHandlerMock.mock.calls[0][0];

    expect(error).toBeInstanceOf(Error);
    expect(error.name).toBe('bbb');
    expect(error.message).toBe('ccc');
    expect(error.stack).toBe('ddd');
  });
});
