import { sleep } from 'parallel-universe';
import { Connection, createEventBridge } from '../main';
import { noop } from '../main/utils';

describe('EventBridge', () => {
  test('sends a request and gets a response when a connection is already initialized', async () => {
    const connection: Connection = {
      post: jest.fn(() => {
        connection.inbox?.push([0, { type: 'bbb', ok: true, key1: 111 }]);
      }),
    };

    const eventBridge = createEventBridge(() => connection);

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true, key1: 111 });

    expect(connection.post).toHaveBeenCalledTimes(1);
    expect(connection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('uses window.racehorseConnection by default', async () => {
    window.racehorseConnection = {
      post: jest.fn(() => {
        window.racehorseConnection?.inbox?.push([0, { type: 'bbb', ok: true }]);
      }),
    };

    const eventBridge = createEventBridge();

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    expect(window.racehorseConnection.post).toHaveBeenCalledTimes(1);
    expect(window.racehorseConnection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('sends a request and gets a response when a connection is not yet initialized', async () => {
    const eventBridge = createEventBridge(() => connection);

    const connection: Connection = {
      post: jest.fn(() => {
        connection?.inbox?.push([0, { type: 'bbb', ok: true, key1: 111 }]);
      }),
    };

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true, key1: 111 });

    expect(connection.post).toHaveBeenCalledTimes(1);
    expect(connection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('listener receives alert events', async () => {
    const listenerMock = jest.fn();

    const eventBridge = createEventBridge(() => connection);

    eventBridge.subscribeToAlerts(listenerMock);

    const connection: Connection = {
      post() {},
    };

    await sleep(200);

    expect(connection.inbox).toBeDefined();

    connection.inbox?.push([null, { type: 'aaa', ok: true, key1: 111 }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa', ok: true, key1: 111 });
  });

  test('listener receives an alert event from the array inbox', async () => {
    const connection: Connection = {
      inbox: [[null, { type: 'aaa', ok: true, key1: 111 }] as const],
      post() {},
    };

    const listenerMock = jest.fn();

    createEventBridge(() => connection).subscribeToAlerts(listenerMock);

    await sleep(200);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa', ok: true, key1: 111 });
  });

  test('applies the plugin', async () => {
    const pluginMock = jest.fn();

    const eventBus = createEventBridge(undefined, pluginMock);

    expect(pluginMock).toHaveBeenCalledTimes(1);
    expect(pluginMock).toHaveBeenNthCalledWith(1, eventBus, noop);
  });

  test('applies the plugin', async () => {
    const pluginMock = jest.fn();

    const eventBus = createEventBridge(undefined, pluginMock);

    expect(eventBus).toEqual({ request: expect.any(Function), subscribe: expect.any(Function) });
    expect(pluginMock).toHaveBeenCalledTimes(1);
    expect(pluginMock).toHaveBeenNthCalledWith(1, eventBus, noop);
  });

  test('plugin receives the listener', async () => {
    const pluginMock = jest.fn();
    const listener = () => undefined;

    const eventBus = createEventBridge(undefined, pluginMock, listener);

    expect(pluginMock).toHaveBeenCalledTimes(1);
    expect(pluginMock).toHaveBeenNthCalledWith(1, eventBus, listener);
  });
});
