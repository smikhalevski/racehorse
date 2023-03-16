import { Connection, createConnectionProvider, createEventBridge, createNetworkManager } from '../main';

describe('createNetworkManager', () => {
  test('returns undefined if network status is unknown', () => {
    const eventBridge = createEventBridge(createConnectionProvider(() => undefined));

    expect(createNetworkManager(eventBridge).online).toBe(undefined);
  });

  test('reads initial online status', () => {
    const connection: Connection = {
      post: jest.fn(() => {
        connection.inboxPubSub!.publish([0, { type: '', ok: true, online: true }]);
      }),
    };

    const networkManager = createNetworkManager(createEventBridge(createConnectionProvider(() => connection)));

    expect(networkManager.online).toBe(true);
  });

  test('calls listener if a network alert arrives', () => {
    const connection: Connection = {
      post: () => undefined,
    };

    const listenerMock = jest.fn();

    const networkManager = createNetworkManager(createEventBridge(createConnectionProvider(() => connection)));

    networkManager.subscribe(listenerMock);

    connection.inboxPubSub!.publish([-1, { type: 'org.racehorse.OnlineStatusChangedAlertEvent', online: true }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);

    expect(networkManager.online).toBe(true);
  });
});
