import { act, renderHook } from '@testing-library/react';
import { ConnectionProviderContext, useEventBridge } from '../main';
import { createElement, ReactNode, StrictMode } from 'react';
import { Connection, ConnectionProvider, createConnectionProvider, Plugin } from 'racehorse';

describe('useEventBridge', () => {
  let connectionMock: Connection | undefined;
  let provideConnection: () => void;
  let connectionProviderMock: ConnectionProvider;

  const connectionProviderWrapper = (props: { children: ReactNode }) =>
    createElement(
      ConnectionProviderContext.Provider,
      { value: connectionProviderMock },
      createElement(StrictMode, null, props.children)
    );

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

  test('returns a copy of the event bus on each render', () => {
    const hook = renderHook(() => useEventBridge(), { wrapper: StrictMode });
    const eventBridge = hook.result.current;

    expect(eventBridge).toEqual({
      waitForConnection: expect.any(Function),
      request: expect.any(Function),
      requestSync: expect.any(Function),
      subscribeToAlerts: expect.any(Function),
      subscribe: expect.any(Function),
    });
  });

  test('returns a copy of the event bus on each render', () => {
    const hook = renderHook(() => useEventBridge(), { wrapper: StrictMode });
    const eventBridge = hook.result.current;

    hook.rerender();

    expect(hook.result.current).not.toBe(eventBridge);
  });

  test('consumes a connection provider from the context', () => {
    const hook = renderHook(() => useEventBridge(), { wrapper: connectionProviderWrapper });

    hook.result.current.request({ type: 'aaa' });

    expect(connectionProviderMock).toHaveBeenCalledTimes(1);
  });

  test('applies the plugin', () => {
    const pluginMock: Plugin<{ foo: string }> = jest.fn(evenBridge => {
      evenBridge.foo = 'aaa';
    });

    const hook = renderHook(() => useEventBridge(pluginMock), { wrapper: connectionProviderWrapper });

    expect(pluginMock).toHaveBeenCalledTimes(2);
    expect(hook.result.current.foo).toBe('aaa');
  });

  test('calls the plugin only on the first render', () => {
    const pluginMock: Plugin<object> = jest.fn();

    const hook = renderHook(() => useEventBridge(pluginMock), { wrapper: connectionProviderWrapper });

    hook.rerender();
    hook.rerender();

    expect(pluginMock).toHaveBeenCalledTimes(2);
  });

  test('calls listeners if plugin notifies the event bridge', () => {
    let notifyCallback: () => void;

    const listenerMock = jest.fn();

    const hook = renderHook(
      () =>
        useEventBridge((eventBridge, notify) => {
          notifyCallback = notify;
        }),
      { wrapper: connectionProviderWrapper }
    );

    hook.result.current.subscribe(listenerMock);

    expect(listenerMock).toHaveBeenCalledTimes(0);

    act(() => notifyCallback());

    expect(listenerMock).toHaveBeenCalledTimes(1);
  });

  test('re-renders the component if plugin notifies the event bridge', () => {
    let notifyCallback: () => void;

    const renderMock = jest.fn(() =>
      useEventBridge((eventBridge, notify) => {
        notifyCallback = notify;
      })
    );

    renderHook(renderMock, { wrapper: connectionProviderWrapper });

    expect(renderMock).toHaveBeenCalledTimes(2);

    act(() => notifyCallback());

    expect(renderMock).toHaveBeenCalledTimes(4);
  });

  test('plugins can subscribe to alerts', () => {
    provideConnection();

    const listenerMock = jest.fn();

    renderHook(
      () =>
        useEventBridge(eventBridge => {
          eventBridge.subscribeToAlerts(listenerMock);
        }),
      { wrapper: connectionProviderWrapper }
    );

    act(() => {
      connectionMock!.inboxPubSub!.publish([-1, { type: 'bbb' }]);
    });

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(listenerMock).toHaveBeenNthCalledWith(1, { type: 'bbb' });
  });
});
