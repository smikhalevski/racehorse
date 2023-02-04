import { sleep } from 'parallel-universe';
import { Connection, createEventBridge } from '../main';

describe('EventBridge', () => {
  test('updates connection status', async () => {
    const eventBridge = createEventBridge({
      connectionProvider: () => connection,
    });

    expect(eventBridge.isConnected()).toBe(false);

    const connection: Connection = {
      post() {},
    };

    await sleep(200);

    expect(eventBridge.isConnected()).toBe(true);
  });

  test('sends a request and gets a response when a connection is already initialized', async () => {
    const connection: Connection = {
      post: jest.fn(() => {
        connection.inbox?.push([0, { type: 'bbb', ok: true, key1: 111 }]);
      }),
    };

    const eventBridge = createEventBridge({
      connectionProvider: () => connection,
    });

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true, key1: 111 });

    expect(connection.post).toHaveBeenCalledTimes(1);
    expect(connection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('sends a request and gets a response when a connection is not yet initialized', async () => {
    const eventBridge = createEventBridge({
      connectionProvider: () => connection,
    });

    const connection: Connection = {
      post: jest.fn(() => {
        connection?.inbox?.push([0, { type: 'bbb', ok: true, key1: 111 }]);
      }),
    };

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true, key1: 111 });

    expect(connection.post).toHaveBeenCalledTimes(1);
    expect(connection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
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

    connection.inbox?.push([null, { type: 'aaa', ok: true, key1: 111 }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa', ok: true, key1: 111 });
  });

  test('listener receives a message from the array inbox', async () => {
    const connection: Connection = {
      inbox: [[null, { type: 'aaa', ok: true, key1: 111 }] as const],
      post() {},
    };

    const listenerMock = jest.fn();

    createEventBridge({ connectionProvider: () => connection }).subscribe(listenerMock);

    await sleep(200);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa', ok: true, key1: 111 });
  });
});
