import { act, renderHook } from '@testing-library/react';
import { NetworkManagerContext, useOnline } from '../main';
import { createElement, StrictMode } from 'react';
import { Connection, createConnectionProvider, createEventBridge, createNetworkManager } from 'racehorse/src/main';

describe('useOnline', () => {
  test('returns undefined if network status is unknown', () => {
    const hook = renderHook(() => useOnline(), { wrapper: StrictMode });

    expect(hook.result.current).toBe(undefined);
  });

  test('reads initial online status', () => {
    const connection: Connection = {
      post: jest.fn(() => {
        connection.inboxPubSub!.publish([0, { type: '', ok: true, online: true }]);
      }),
    };

    const networkManager = createNetworkManager(createEventBridge(createConnectionProvider(() => connection)));

    const hook = renderHook(() => useOnline(), {
      wrapper: ({ children }) => createElement(NetworkManagerContext.Provider, { value: networkManager }, children),
    });

    expect(hook.result.current).toBe(true);
  });

  test('re-renders the network alert arrives', () => {
    const connection: Connection = {
      post: () => undefined,
    };

    const networkManager = createNetworkManager(createEventBridge(createConnectionProvider(() => connection)));

    const hookMock = jest.fn(() => useOnline());

    const hook = renderHook(hookMock, {
      wrapper: ({ children }) => createElement(NetworkManagerContext.Provider, { value: networkManager }, children),
    });

    act(() => {
      connection.inboxPubSub!.publish([-1, { type: 'org.racehorse.OnlineStatusChangedAlertEvent', online: true }]);
    });

    expect(hookMock).toHaveBeenCalledTimes(2);
    expect(hook.result.current).toBe(true);
  });
});
