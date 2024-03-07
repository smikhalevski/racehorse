import { createJoiner } from '../main';

describe('createJoiner', () => {
  test('invokes the action', async () => {
    const actionMock = jest.fn(() => Promise.resolve('aaa'));
    const joiner = createJoiner();

    expect(joiner.isPending()).toBe(false);

    const promise = joiner.join(actionMock);

    expect(joiner.isPending()).toBe(true);
    expect(actionMock).toHaveBeenCalledTimes(1);

    await expect(promise).resolves.toBe('aaa');

    expect(actionMock).toHaveBeenCalledTimes(1);
  });

  test('joins the pending action', async () => {
    const joiner = createJoiner();

    joiner.join(() => Promise.resolve('aaa'));

    const promise = joiner.join(() => Promise.resolve('bbb'));

    await expect(promise).resolves.toBe('aaa');
  });
});
