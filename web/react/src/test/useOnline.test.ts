import { act, renderHook } from '@testing-library/react';
import { NetworkManagerContext, useOnline } from '../main';
import { createElement, StrictMode } from 'react';
import { sleep } from 'parallel-universe';
import { Connection, createEventBridge, createNetworkManager } from 'racehorse';

describe('useOnline', () => {
  test('returns undefined if network status is unknown', () => {
    const hook = renderHook(() => useOnline(), { wrapper: StrictMode });

    expect(hook.result.current).toBe(undefined);
  });

  test('reads initial online status', async () => {
    const connection: Connection = {
      post: jest.fn(() => {
        setTimeout(() => connection.inbox!.publish([111, { type: '', ok: true, online: true }]), 0);
        return 111;
      }),
    };

    const eventBridge = createEventBridge(() => connection);
    await eventBridge.connect();

    const networkManager = createNetworkManager(eventBridge);

    const hookMock = jest.fn(() => useOnline());

    const hook = renderHook(hookMock, {
      wrapper: ({ children }) => createElement(NetworkManagerContext.Provider, { value: networkManager }, children),
    });

    await sleep(100);

    expect(hookMock).toHaveBeenCalledTimes(2);
    expect(hook.result.current).toBe(true);
  });

  test('re-renders the network alert arrives', async () => {
    const connection: Connection = {
      post: () => 222,
    };

    const eventBridge = createEventBridge(() => connection);
    await eventBridge.connect();

    const networkManager = createNetworkManager(eventBridge);

    const hookMock = jest.fn(() => useOnline());

    const hook = renderHook(hookMock, {
      wrapper: ({ children }) => createElement(NetworkManagerContext.Provider, { value: networkManager }, children),
    });

    act(() => {
      connection.inbox!.publish([-1, { type: 'org.racehorse.OnlineStatusChangedAlertEvent', online: true }]);
    });

    expect(hookMock).toHaveBeenCalledTimes(2);
    expect(hook.result.current).toBe(true);
  });
});
