import { createConnectionProvider, createEventBridge, networkPlugin } from '../main';

describe('networkPlugin', () => {
  test('returns undefined if network status is unknown', () => {
    expect(createEventBridge(networkPlugin, createConnectionProvider()).online).toBe(undefined);
  });

  test('reads initial online status', () => {
    window.racehorseConnection = {
      post: jest.fn(() => {
        window.racehorseConnection!.inboxPubSub!.publish([0, { type: '', ok: true, online: true }]);
      }),
    };

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(networkPlugin, createConnectionProvider());

    expect(listenerMock).not.toHaveBeenCalled();
    expect(eventBridge.online).toBe(true);
  });

  test('calls listener if a network alert arrives', () => {
    window.racehorseConnection = {
      post: () => undefined,
    };

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(networkPlugin, createConnectionProvider());

    eventBridge.subscribe(listenerMock);

    window.racehorseConnection.inboxPubSub!.publish([
      -1,
      { type: 'org.racehorse.OnlineStatusChangedAlertEvent', online: true },
    ]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(eventBridge.online).toBe(true);
  });
});
