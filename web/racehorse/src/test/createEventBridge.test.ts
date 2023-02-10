import { createEventBridge } from '../main';

describe('EventBridge', () => {
  beforeEach(() => {
    window.racehorseConnection = undefined;
  });

  test('sends a request and gets a response when a connection is already initialized', async () => {
    window.racehorseConnection = {
      post: jest.fn(() => {
        window.racehorseConnection!.inboxChannel!.publish([0, { type: 'bbb', ok: true }]);
      }),
    };

    const eventBridge = createEventBridge();

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    expect(window.racehorseConnection.post).toHaveBeenCalledTimes(1);
    expect(window.racehorseConnection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('sends a request and gets a response when a connection is not yet initialized', async () => {
    const eventBridge = createEventBridge();

    window.racehorseConnection = {
      post: jest.fn(() => {
        window.racehorseConnection!.inboxChannel!.publish([0, { type: 'bbb', ok: true }]);
      }),
    };

    await expect(eventBridge.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    expect(window.racehorseConnection.post).toHaveBeenCalledTimes(1);
    expect(window.racehorseConnection.post).toHaveBeenNthCalledWith(1, 0, '{"type":"aaa"}');
  });

  test('supports multiple bridges', async () => {
    window.racehorseConnection = {
      post: jest
        .fn()
        .mockImplementationOnce(() => {
          window.racehorseConnection!.inboxChannel!.publish([0, { type: 'bbb', ok: true }]);
        })
        .mockImplementationOnce(() => {
          window.racehorseConnection!.inboxChannel!.publish([1, { type: 'bbb', ok: true }]);
        }),
    };

    const eventBridge1 = createEventBridge();

    await expect(eventBridge1.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });

    const eventBridge2 = createEventBridge();

    await expect(eventBridge2.request({ type: 'aaa' })).resolves.toEqual({ type: 'bbb', ok: true });
  });

  test('listener receives alert events', () => {
    window.racehorseConnection = {
      post() {},
    };

    const listenerMock = jest.fn();

    const eventBridge = createEventBridge();

    eventBridge.subscribeToAlerts(listenerMock);

    window.racehorseConnection.inboxChannel?.publish([-1, { type: 'aaa' }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa' });
  });

  // test('listener receives an alert event from the array inbox', async () => {
  //   const connection: Connection = {
  //     inbox: [[null, { type: 'aaa', ok: true, key1: 111 }] as const],
  //     post() {},
  //   };
  //
  //   const listenerMock = jest.fn();
  //
  //   createEventBridge(() => connection).subscribeToAlerts(listenerMock);
  //
  //   await sleep(200);
  //
  //   expect(listenerMock).toHaveBeenCalledTimes(1);
  //   expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'aaa', ok: true, key1: 111 });
  // });
  //
  // test('applies the plugin', async () => {
  //   const pluginMock = jest.fn();
  //
  //   const eventBus = createEventBridge(undefined, pluginMock);
  //
  //   expect(pluginMock).toHaveBeenCalledTimes(1);
  //   expect(pluginMock).toHaveBeenNthCalledWith(1, eventBus, noop);
  // });
  //
  // test('applies the plugin', async () => {
  //   const pluginMock = jest.fn();
  //
  //   const eventBus = createEventBridge(undefined, pluginMock);
  //
  //   expect(eventBus).toEqual({ request: expect.any(Function), subscribeToAlerts: expect.any(Function) });
  //   expect(pluginMock).toHaveBeenCalledTimes(1);
  //   expect(pluginMock).toHaveBeenNthCalledWith(1, eventBus, noop);
  // });
  //
  // test('plugin receives the listener', async () => {
  //   const pluginMock = jest.fn();
  //   const listener = () => undefined;
  //
  //   const eventBus = createEventBridge(undefined, pluginMock, listener);
  //
  //   expect(pluginMock).toHaveBeenCalledTimes(1);
  //   expect(pluginMock).toHaveBeenNthCalledWith(1, eventBus, listener);
  // });
});
