import { Connection, createActionsManager, createEventBridge } from '../main';

describe('createActionsManager', () => {
  test('returns true if opened', async () => {
    const connection: Connection = {
      post: jest.fn(() => {
        setTimeout(() => connection.inbox!.publish([111, { type: '', ok: true, opened: true }]), 0);
        return 111;
      }),
    };

    const eventBridge = createEventBridge(() => connection);

    const requestSpy = jest.spyOn(eventBridge, 'request');

    await expect(createActionsManager(eventBridge).openUrl('aaa')).resolves.toBe(true);

    expect(requestSpy).toHaveBeenCalledTimes(1);
    expect(requestSpy).toHaveBeenNthCalledWith(1, {
      type: 'org.racehorse.OpenUrlRequestEvent',
      url: 'aaa',
    });
  });

  test('returns false if not opened', async () => {
    const connection: Connection = {
      post: jest.fn(() => {
        setTimeout(() => connection.inbox!.publish([111, { type: '', ok: true, opened: false }]), 0);
        return 111;
      }),
    };

    await expect(createActionsManager(createEventBridge(() => connection)).openUrl('aaa')).resolves.toBe(false);
  });
});
