import { Connection, createEventBridge, Plugin } from '../main';

describe('EventBridge', () => {
  let connection: Connection;

  beforeEach(() => {
    connection = {
      post: jest.fn(() => {
        connection.inboxChannel!.publish([0, { type: 'bbb', ok: true }]);
      }),
    };
  });

  test('does not call provider during initialization', () => {
    const connectionProviderMock = jest.fn(() => connection);

    createEventBridge(undefined, connectionProviderMock);

    expect(connectionProviderMock).not.toHaveBeenCalled();
  });

  test('sends async request if connection is available', async () => {
    const eventBridge = createEventBridge(undefined, () => connection);

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    expect(connection.post).toHaveBeenCalledTimes(1);
    expect(connection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('sends async request if connection is deferred', async () => {
    const eventBridge = createEventBridge(undefined, () => Promise.resolve(connection));

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    expect(connection.post).toHaveBeenCalledTimes(1);
    expect(connection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('sends sync request if connection is available', () => {
    const eventBridge = createEventBridge(undefined, () => connection);

    expect(eventBridge.requestSync({ type: 'aaa' })).toEqual({ type: 'bbb', ok: true });

    expect(connection.post).toHaveBeenCalledTimes(1);
    expect(connection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('does not send sync request if connection is deferred', () => {
    const eventBridge = createEventBridge(undefined, () => Promise.resolve(connection));

    expect(eventBridge.requestSync({ type: 'aaa' })).toBe(undefined);

    expect(connection.post).not.toHaveBeenCalled();
  });

  test('sync request returns undefined if response is async', () => {
    connection.post = jest.fn(() => {
      Promise.resolve().then(() => {
        connection.inboxChannel!.publish([0, { type: 'bbb', ok: true }]);
      });
    });

    const eventBridge = createEventBridge(undefined, () => connection);

    expect(eventBridge.requestSync({ type: 'aaa' })).toBe(undefined);

    expect(connection.post).toHaveBeenCalledTimes(1);
    expect(connection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('subscribes a listener to an inbox channel is connection is available', () => {
    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(undefined, () => connection);

    eventBridge.subscribeToAlerts(listenerMock);

    connection.inboxChannel!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa' });
  });

  test('subscribes a listener to an inbox channel is connection is deferred', async () => {
    const listenerMock = jest.fn();
    const connectionPromise = Promise.resolve(connection);
    const eventBridge = createEventBridge(undefined, () => connectionPromise);

    eventBridge.subscribeToAlerts(listenerMock);

    await connectionPromise;

    connection.inboxChannel!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa' });
  });

  test('unsubscribes a listener from an inbox channel is connection is available', () => {
    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(undefined, () => connection);

    eventBridge.subscribeToAlerts(listenerMock)();

    connection.inboxChannel!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).not.toHaveBeenCalled();
  });

  test('unsubscribes a listener from an inbox channel is connection is deferred', async () => {
    const listenerMock = jest.fn();
    const connectionPromise = Promise.resolve(connection);
    const eventBridge = createEventBridge(undefined, () => connectionPromise);

    eventBridge.subscribeToAlerts(listenerMock)();

    await connectionPromise;

    expect(connection.inboxChannel).toBe(undefined);
  });

  test('unsubscribes a listener from an inbox channel if connection was deferred', async () => {
    const listenerMock = jest.fn();
    const connectionPromise = Promise.resolve(connection);
    const eventBridge = createEventBridge(undefined, () => connectionPromise);

    const unsubscribe = eventBridge.subscribeToAlerts(listenerMock);

    await connectionPromise;

    connection.inboxChannel!.publish([-1, { type: 'aaa' }]);

    unsubscribe();

    connection.inboxChannel!.publish([-1, { type: 'bbb' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa' });
  });

  test('supports multiple bridges', async () => {
    connection.post = jest
      .fn()
      .mockImplementationOnce(() => {
        connection.inboxChannel!.publish([0, { type: 'bbb', ok: true }]);
      })
      .mockImplementationOnce(() => {
        connection.inboxChannel!.publish([1, { type: 'bbb', ok: true }]);
      });

    const eventBridge1 = createEventBridge(undefined, () => connection);

    await expect(eventBridge1.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    const eventBridge2 = createEventBridge(undefined, () => connection);

    await expect(eventBridge2.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });
  });

  test('applies a plugin', () => {
    const pluginMock = jest.fn();

    const eventBridge = createEventBridge(pluginMock, () => connection);

    expect(pluginMock).toHaveBeenCalledTimes(1);
    expect(pluginMock).toHaveBeenNthCalledWith(1, eventBridge, expect.any(Function));
  });

  test('plugin notifies bridge listeners', () => {
    const listenerMock = jest.fn();
    const plugin: Plugin<object> = (eventBridge, listener) => {
      eventBridge.subscribeToAlerts(() => {
        listener();
      });
    };

    const eventBridge = createEventBridge(plugin, () => connection);

    eventBridge.subscribeToBridge(listenerMock);

    connection.inboxChannel!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
  });

  test('plugin does not notify unsubscribed bridge listeners', () => {
    const listenerMock = jest.fn();
    const plugin: Plugin<object> = (eventBridge, listener) => {
      eventBridge.subscribeToAlerts(() => {
        listener();
      });
    };

    const eventBridge = createEventBridge(plugin, () => connection);

    eventBridge.subscribeToBridge(listenerMock)();

    connection.inboxChannel!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).not.toHaveBeenCalled();
  });
});
