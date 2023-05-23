import { Connection, createEventBridge } from '../main';

describe('createEventBridge', () => {
  let connectionMock: Connection | undefined;
  let provideConnection: () => void;
  let connectionProviderMock = jest.fn(() => connectionMock);

  beforeEach(() => {
    connectionProviderMock.mockClear();

    connectionMock = undefined;

    provideConnection = () => {
      connectionMock = {
        post: jest.fn(() => {
          setTimeout(() => connectionMock!.inbox!.publish([111, { type: 'bbb' }]), 0);
          return '111';
        }),
      };
    };
  });

  test('does not call connection provider during initialization', () => {
    createEventBridge(connectionProviderMock);

    expect(connectionProviderMock).not.toHaveBeenCalled();
  });

  test('posts a request if connection is available', async () => {
    provideConnection();
    const eventBridge = createEventBridge(connectionProviderMock);

    await expect(eventBridge.requestAsync({ type: 'aaa' })).resolves.toEqual({ type: 'bbb' });

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
    expect(connectionMock!.post).toHaveBeenNthCalledWith(1, '{"type":"aaa"}');
  });

  test('posts a request if connection is deferred', async () => {
    const eventBridge = createEventBridge(connectionProviderMock);

    const promise = eventBridge.requestAsync({ type: 'aaa' });

    setTimeout(provideConnection, 100);

    await expect(promise).resolves.toEqual({ type: 'bbb' });

    expect(connectionMock!.post).toHaveBeenCalledTimes(1);
    expect(connectionMock!.post).toHaveBeenNthCalledWith(1, '{"type":"aaa"}');
  });

  test('subscribes an alert listener', async () => {
    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(connectionProviderMock);

    eventBridge.subscribe(listenerMock);

    provideConnection();

    await eventBridge.connect();

    connectionMock!.inbox!.publish([-2, { type: 'aaa' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa' });
  });

  test('unsubscribes an alert listener', async () => {
    provideConnection();

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(connectionProviderMock);

    await eventBridge.connect();

    eventBridge.subscribe(listenerMock)();

    connectionMock!.inbox!.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).not.toHaveBeenCalled();
  });

  test('multiple bridges can coexist', async () => {
    provideConnection();

    connectionMock!.post = jest
      .fn()
      .mockImplementationOnce(() => {
        setTimeout(() => connectionMock!.inbox!.publish([111, { type: 'bbb' }]), 0);
        return 111;
      })
      .mockImplementationOnce(() => {
        setTimeout(() => connectionMock!.inbox!.publish([222, { type: 'bbb' }]), 0);
        return 222;
      });

    const eventBridge1 = createEventBridge(connectionProviderMock);

    await eventBridge1.connect();

    await expect(eventBridge1.requestAsync({ type: 'aaa' })).resolves.toEqual({ type: 'bbb' });

    const eventBridge2 = createEventBridge(connectionProviderMock);

    await eventBridge2.connect();

    await expect(eventBridge2.requestAsync({ type: 'aaa' })).resolves.toEqual({ type: 'bbb' });
  });
});
