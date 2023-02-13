import { Connection, ConnectionProvider, createConnectionProvider, createEventBridge, Plugin } from '../main';

describe('EventBridge', () => {
  let connectionMock: Connection | undefined;
  let provideConnection: () => void;
  let connectionProviderMock: ConnectionProvider;

  beforeEach(() => {
    connectionMock = undefined;

    provideConnection = () => {
      connectionMock = {
        post: jest.fn(() => {
          connectionMock!.inboxPubSub!.publish([0, { type: 'bbb', ok: true }]);
        }),
      };
    };

    connectionProviderMock = jest.fn(createConnectionProvider(() => connectionMock, 0, Infinity));
  });

  test('does not call provider during initialization', () => {
    createEventBridge(undefined, connectionProviderMock);

    expect(connectionProviderMock).not.toHaveBeenCalled();
  });

  test('sends async request if connection is available', async () => {
    provideConnection();
    const eventBridge = createEventBridge(undefined, connectionProviderMock);

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
    expect(connectionMock!.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('sends async request if connection is deferred', async () => {
    const eventBridge = createEventBridge(undefined, connectionProviderMock);

    provideConnection();

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
    expect(connectionMock!.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('sends sync request if connection is available', () => {
    provideConnection();
    const eventBridge = createEventBridge(undefined, connectionProviderMock);

    expect(eventBridge.requestSync({ type: 'aaa' })).toEqual({ type: 'bbb', ok: true });

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
    expect(connectionMock!.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('does not send sync request if connection is deferred', () => {
    const eventBridge = createEventBridge(undefined, connectionProviderMock);

    expect(eventBridge.requestSync({ type: 'aaa' })).toBe(undefined);

    provideConnection();

    expect(connectionMock!.post).not.toHaveBeenCalled();
  });

  test('sync request returns undefined if response is async', () => {
    provideConnection();

    connectionMock!.post = jest.fn(() => {
      Promise.resolve().then(() => {
        connectionMock!.inboxPubSub!.publish([0, { type: 'bbb', ok: true }]);
      });
    });

    const eventBridge = createEventBridge(undefined, connectionProviderMock);

    expect(eventBridge.requestSync({ type: 'aaa' })).toBe(undefined);

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
    expect(connectionMock!.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('subscribes a listener to an inbox pubsub if connection is available', () => {
    provideConnection();

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(undefined, connectionProviderMock);

    eventBridge.watchForAlerts(listenerMock);

    connectionMock!.inboxPubSub!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa' });
  });

  test('subscribes a listener to an inbox pubsub if connection is deferred', async () => {
    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(undefined, connectionProviderMock);

    eventBridge.watchForAlerts(listenerMock);

    provideConnection();

    await eventBridge.waitForConnection();

    connectionMock!.inboxPubSub!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa' });
  });

  test('unsubscribes a listener from an inbox pubsub if connection is available', () => {
    provideConnection();

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(undefined, connectionProviderMock);

    eventBridge.watchForAlerts(listenerMock)();

    connectionMock!.inboxPubSub!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).not.toHaveBeenCalled();
  });

  test('unsubscribes a listener from an inbox pubsub if connection is deferred', async () => {
    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(undefined, connectionProviderMock);

    eventBridge.watchForAlerts(listenerMock)();

    provideConnection();

    await eventBridge.waitForConnection();

    expect(connectionMock!.inboxPubSub).toBe(undefined);
  });

  test('unsubscribes a listener from an inbox pubsub if connection was deferred', async () => {
    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(undefined, connectionProviderMock);

    const unsubscribe = eventBridge.watchForAlerts(listenerMock);

    provideConnection();

    await eventBridge.waitForConnection();

    connectionMock!.inboxPubSub!.publish([-1, { type: 'aaa' }]);

    unsubscribe();

    connectionMock!.inboxPubSub!.publish([-1, { type: 'bbb' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa' });
  });

  test('supports multiple bridges', async () => {
    provideConnection();

    connectionMock!.post = jest
      .fn()
      .mockImplementationOnce(() => {
        connectionMock!.inboxPubSub!.publish([0, { type: 'bbb', ok: true }]);
      })
      .mockImplementationOnce(() => {
        connectionMock!.inboxPubSub!.publish([1, { type: 'bbb', ok: true }]);
      });

    const eventBridge1 = createEventBridge(undefined, connectionProviderMock);

    await expect(eventBridge1.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    const eventBridge2 = createEventBridge(undefined, connectionProviderMock);

    await expect(eventBridge2.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });
  });

  test('applies a plugin', () => {
    const pluginMock = jest.fn();

    const eventBridge = createEventBridge(pluginMock, connectionProviderMock);

    expect(pluginMock).toHaveBeenCalledTimes(1);
    expect(pluginMock).toHaveBeenNthCalledWith(1, eventBridge, expect.any(Function));
  });

  test('plugin notifies bridge listeners', () => {
    provideConnection();

    const listenerMock = jest.fn();
    const plugin: Plugin<object> = (eventBridge, listener) => {
      eventBridge.watchForAlerts(() => {
        listener();
      });
    };

    const eventBridge = createEventBridge(plugin, connectionProviderMock);

    eventBridge.subscribe(listenerMock);

    connectionMock!.inboxPubSub!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
  });

  test('plugin does not notify unsubscribed bridge listeners', () => {
    provideConnection();

    const listenerMock = jest.fn();
    const plugin: Plugin<object> = (eventBridge, listener) => {
      eventBridge.watchForAlerts(() => {
        listener();
      });
    };

    const eventBridge = createEventBridge(plugin, connectionProviderMock);

    eventBridge.subscribe(listenerMock)();

    connectionMock!.inboxPubSub!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).not.toHaveBeenCalled();
  });

  test('waits for connection to be available', async () => {
    const eventBridge = createEventBridge(undefined, connectionProviderMock);

    expect(eventBridge.requestSync({ type: 'aaa' })).toBe(undefined);

    provideConnection();

    await eventBridge.waitForConnection();

    expect(eventBridge.requestSync({ type: 'aaa' })).toEqual({ ok: true, type: 'bbb' });

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
  });
});
