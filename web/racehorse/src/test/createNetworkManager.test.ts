import { sleep } from 'parallel-universe';
import { Connection, createEventBridge, createNetworkManager } from '../main';

describe('createNetworkManager', () => {
  test('returns undefined if network status is unknown', () => {
    const eventBridge = createEventBridge(() => undefined);

    expect(createNetworkManager(eventBridge).isOnline).toBe(undefined);
  });

  test('reads initial online status', async () => {
    const connection: Connection = {
      post: jest.fn(() => {
        setTimeout(() => {
          connection.inbox!.publish([111, { type: '', ok: true, isOnline: true }]);
        }, 0);

        return 111;
      }),
    };

    const eventBridge = createEventBridge(() => connection);
    await eventBridge.connect();

    const networkManager = createNetworkManager(eventBridge);

    expect(networkManager.isOnline).toBe(undefined);

    await sleep(100);

    expect(networkManager.isOnline).toBe(true);
  });

  test('calls listener if a network alert arrives', async () => {
    const connection: Connection = {
      post: () => 222,
    };

    const listenerMock = jest.fn();

    const eventBridge = createEventBridge(() => connection);
    await eventBridge.connect();

    const networkManager = createNetworkManager(eventBridge);

    networkManager.subscribe(listenerMock);

    connection.inbox!.publish([-1, { type: 'org.racehorse.OnlineStatusChangedAlertEvent', isOnline: true }]);

    expect(listenerMock).toHaveBeenCalledTimes(1);

    expect(networkManager.isOnline).toBe(true);
  });
});
