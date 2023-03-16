import { Connection, createActionsManager, createEventBridge } from '../main';

describe('createActionsManager', () => {
  test('returns true if opened', () => {
    const connection: Connection = {
      post: jest.fn(() => {
        connection.inboxPubSub!.publish([0, { type: '', ok: true, opened: true }]);
      }),
    };

    const eventBridge = createEventBridge(() => connection);

    const requestSyncSpy = jest.spyOn(eventBridge, 'requestSync');

    const actionsManager = createActionsManager(eventBridge);

    const result = actionsManager.openUrl('aaa');

    expect(result).toBe(true);

    expect(requestSyncSpy).toHaveBeenCalledTimes(1);
    expect(requestSyncSpy).toHaveBeenNthCalledWith(1, { type: 'org.racehorse.OpenUrlRequestEvent', url: 'aaa' });
  });

  test('returns false if not opened', () => {
    const connection: Connection = {
      post: jest.fn(() => {
        connection.inboxPubSub!.publish([0, { type: '', ok: true, opened: false }]);
      }),
    };

    const result = createActionsManager(createEventBridge(() => connection)).openUrl('aaa');

    expect(result).toBe(false);
  });
});
