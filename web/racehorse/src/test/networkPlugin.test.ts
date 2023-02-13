import { Connection, createConnectionProvider, createEventBridge, networkPlugin } from '../main';

describe('networkPlugin', () => {
  test('returns undefined if network status is unknown', () => {
    const eventBridge = createEventBridge(
      networkPlugin,
      createConnectionProvider(() => undefined)
    );

    expect(eventBridge.online).toBe(undefined);
  });

  test('reads initial online status', () => {
    const connection: Connection = {
      post: jest.fn(() => {
        connection.inboxPubSub!.publish([0, { type: '', ok: true, online: true }]);
      }),
    };

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(
      networkPlugin,
      createConnectionProvider(() => connection)
    );

    expect(listenerMock).not.toHaveBeenCalled();
    expect(eventBridge.online).toBe(true);
  });

  test('calls listener if a network alert arrives', () => {
    const connection: Connection = {
      post: () => undefined,
    };

    const listenerMock = jest.fn();
    const eventBridge = createEventBridge(
      networkPlugin,
      createConnectionProvider(() => connection)
    );

    eventBridge.subscribe(listenerMock);

    connection.inboxPubSub!.publish([-1, { type: 'org.racehorse.OnlineStatusChangedAlertEvent', online: true }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);
    expect(eventBridge.online).toBe(true);
  });
});
