import { Connection, ConnectionProvider, createConnectionProvider, createEventBridge } from '../main';

describe('createEventBridge', () => {
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

    connectionProviderMock = jest.fn(createConnectionProvider(() => connectionMock));
  });

  test('does not call provider during initialization', () => {
    createEventBridge(connectionProviderMock);

    expect(connectionProviderMock).not.toHaveBeenCalled();
  });

  test('sends async request if connection is available', async () => {
    provideConnection();
    const eventBridge = createEventBridge(connectionProviderMock);

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
    expect(connectionMock!.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('sends async request if connection is deferred', async () => {
    const eventBridge = createEventBridge(connectionProviderMock);

    provideConnection();

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
    expect(connectionMock!.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('sends sync request if connection is available', () => {
    provideConnection();
    const eventBridge = createEventBridge(connectionProviderMock);

    expect(eventBridge.requestSync({ type: 'aaa' })).toEqual({ type: 'bbb', ok: true });

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
    expect(connectionMock!.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('does not send sync request if connection is deferred', () => {
    const eventBridge = createEventBridge(connectionProviderMock);

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

    const eventBridge = createEventBridge(connectionProviderMock);

    expect(eventBridge.requestSync({ type: 'aaa' })).toBe(undefined);

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
    expect(connectionMock!.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('subscribes a listener to an inbox pubsub if connection is available', () => {
    provideConnection();

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(connectionProviderMock);

    eventBridge.subscribe(listenerMock);

    connectionMock!.inboxPubSub!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa' });
  });

  test('subscribes a listener to an inbox pubsub if connection is deferred', async () => {
    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(connectionProviderMock);

    eventBridge.subscribe(listenerMock);

    provideConnection();

    await eventBridge.connect();

    connectionMock!.inboxPubSub!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa' });
  });

  test('unsubscribes a listener from an inbox pubsub if connection is available', () => {
    provideConnection();

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(connectionProviderMock);

    eventBridge.subscribe(listenerMock)();

    connectionMock!.inboxPubSub!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).not.toHaveBeenCalled();
  });

  test('unsubscribes a listener from an inbox pubsub if connection is deferred', async () => {
    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(connectionProviderMock);

    eventBridge.subscribe(listenerMock)();

    provideConnection();

    await eventBridge.connect();

    connectionMock!.inboxPubSub!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).not.toHaveBeenCalled();
  });

  test('unsubscribes a listener from an inbox pubsub if connection was deferred', async () => {
    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(connectionProviderMock);

    const unsubscribe = eventBridge.subscribe(listenerMock);

    provideConnection();

    await eventBridge.connect();

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

    const eventBridge1 = createEventBridge(connectionProviderMock);

    await expect(eventBridge1.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    const eventBridge2 = createEventBridge(connectionProviderMock);

    await expect(eventBridge2.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });
  });

  test('waits for connection to be available', async () => {
    const eventBridge = createEventBridge(connectionProviderMock);

    expect(eventBridge.requestSync({ type: 'aaa' })).toBe(undefined);

    provideConnection();

    await eventBridge.connect();

    expect(eventBridge.requestSync({ type: 'aaa' })).toEqual({ ok: true, type: 'bbb' });

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
  });
});
