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

  describe('subscribe', () => {
    test('subscribes a notify listener', async () => {
      const listenerMock = jest.fn();
      const eventBridge = createEventBridge(connectionProviderMock);

      eventBridge.subscribe(listenerMock);

      provideConnection();

      await eventBridge.connect();

      connectionMock!.inbox!.publish([-2, { type: 'aaa' }]);

      expect(listenerMock).toHaveBeenCalledTimes(1);
      expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa' });
    });

    test('unsubscribes a notify listener', async () => {
      provideConnection();

      const listenerMock = jest.fn();
      const eventBridge = createEventBridge(connectionProviderMock);

      await eventBridge.connect();

      eventBridge.subscribe(listenerMock)();

      connectionMock!.inbox!.publish([-1, { type: 'aaa' }]);

      expect(listenerMock).not.toHaveBeenCalled();
    });

    test('subscribes a to an event type', async () => {
      const listenerMock = jest.fn();
      const eventBridge = createEventBridge(connectionProviderMock);

      eventBridge.subscribe('bbb', listenerMock);

      provideConnection();

      await eventBridge.connect();

      connectionMock!.inbox!.publish([-2, { type: 'aaa' }]);
      connectionMock!.inbox!.publish([-2, { type: 'bbb', payload: 333 }]);
      connectionMock!.inbox!.publish([-2, { type: 'ccc' }]);

      expect(listenerMock).toHaveBeenCalledTimes(1);
      expect(listenerMock).toHaveBeenNthCalledWith(1, 333);
    });
  });

  describe('requestAsync', () => {
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

    test('rejects if an exception is published', async () => {
      provideConnection();

      connectionMock!.post = jest.fn().mockImplementationOnce(() => {
        setTimeout(
          () =>
            connectionMock!.inbox!.publish([
              111,
              {
                type: 'org.racehorse.ExceptionEvent',
                payload: { name: 'foo', message: 'bar', stack: 'baz' },
              },
            ]),
          0
        );
        return 111;
      });

      const eventBridge = createEventBridge(connectionProviderMock);

      await expect(eventBridge.requestAsync({ type: 'aaa' })).rejects.toEqual(new Error('bar'));
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

  describe('request', () => {
    test('throws if connection is not established', () => {
      const eventBridge = createEventBridge(connectionProviderMock);

      expect(() => eventBridge.request({ type: 'aaa' })).toThrow(new Error('Expected an established connection'));
    });

    test('posts a sync request', () => {
      provideConnection();

      connectionMock!.post = jest.fn().mockImplementationOnce(() => '{"type":"bbb"}');

      const eventBridge = createEventBridge(connectionProviderMock);

      expect(eventBridge.request({ type: 'aaa' })).toEqual({ type: 'bbb' });

      expect(connectionProviderMock).toHaveBeenCalledTimes(1);

      expect(connectionMock!.post).toHaveBeenCalledTimes(1);
      expect(connectionMock!.post).toHaveBeenNthCalledWith(1, '{"type":"aaa"}');
    });

    test('establishes connection only once', () => {
      provideConnection();

      connectionMock!.post = jest.fn().mockImplementation(() => '{"type":"bbb"}');

      const eventBridge = createEventBridge(connectionProviderMock);

      eventBridge.request({ type: 'aaa' });
      eventBridge.request({ type: 'aaa' });

      expect(connectionProviderMock).toHaveBeenCalledTimes(1);
    });

    test('throws if response is async', () => {
      provideConnection();

      const eventBridge = createEventBridge(connectionProviderMock);

      expect(() => eventBridge.request({ type: 'aaa' })).toThrow(new Error('Expected a synchronous response'));
    });

    test('throws if an exception is returned', () => {
      provideConnection();

      connectionMock!.post = jest.fn().mockImplementationOnce(() =>
        JSON.stringify({
          type: 'org.racehorse.ExceptionEvent',
          payload: { name: 'foo', message: 'bar', stack: 'baz' },
        })
      );

      const eventBridge = createEventBridge(connectionProviderMock);

      expect(() => eventBridge.request({ type: 'aaa' })).toThrow(new Error('bar'));
    });
  });
});
