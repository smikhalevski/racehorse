import { actionsPlugin, Connection, createEventBridge } from '../main';

describe('actionsPlugin', () => {
  test('returns true if opened', async () => {
    const connection: Connection = {
      post: jest.fn(() => {
        connection.inboxPubSub!.publish([0, { type: '', ok: true, opened: true }]);
      }),
    };

    const eventBridge = createEventBridge(actionsPlugin, () => connection);

    eventBridge.request = jest.fn(eventBridge.request);

    const result = eventBridge.openUrl('aaa');

    expect(result).toBeInstanceOf(Promise);

    await expect(result).resolves.toBe(true);

    expect(eventBridge.request).toHaveBeenCalledTimes(1);
    expect(eventBridge.request).toHaveBeenNthCalledWith(1, { type: 'org.racehorse.OpenUrlRequestEvent', url: 'aaa' });
  });

  test('returns false if not opened', async () => {
    const connection: Connection = {
      post: jest.fn(() => {
        connection.inboxPubSub!.publish([0, { type: '', ok: true, opened: false }]);
      }),
    };

    const result = createEventBridge(actionsPlugin, () => connection).openUrl('aaa');

    expect(result).toBeInstanceOf(Promise);

    await expect(result).resolves.toBe(false);
  });
});
