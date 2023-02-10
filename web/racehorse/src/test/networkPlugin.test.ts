import { sleep } from 'parallel-universe';
import { createEventBridge, networkPlugin } from '../main';

describe('networkPlugin', () => {
  test('returns undefined if network status is unknown', () => {
    expect(createEventBridge(() => undefined, networkPlugin).online).toBe(undefined);
  });

  test('updates online status and calls listener', async () => {
    window.racehorseConnection = {
      post: jest.fn(() => {
        window.racehorseConnection?.inbox?.push([0, { type: '', ok: true, online: true }]);
      }),
    };

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(undefined, networkPlugin, listenerMock);

    await sleep(100);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(eventBridge.online).toBe(true);
  });

  test('calls listener if a network alert arrives', async () => {
    window.racehorseConnection = {
      post: () => undefined,
    };

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(undefined, networkPlugin, listenerMock);

    await sleep(100);

    window.racehorseConnection.inbox?.push([
      null,
      { type: 'org.racehorse.OnlineStatusChangedAlertEvent', online: true },
    ]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(eventBridge.online).toBe(true);
  });

  test('unsubscribes the listener', async () => {
    window.racehorseConnection = {
      post: () => undefined,
    };

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge();

    networkPlugin(eventBridge as any, listenerMock)?.();

    await sleep(100);

    window.racehorseConnection.inbox?.push([
      null,
      { type: 'org.racehorse.OnlineStatusChangedAlertEvent', online: true },
    ]);

    expect(listenerMock).not.toHaveBeenCalled();
    expect((eventBridge as any).online).toBe(undefined);
  });
});
