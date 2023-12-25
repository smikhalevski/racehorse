import { createJoiner } from '../main';

describe('createJoiner', () => {
  test('invokes the operation', async () => {
    const operationMock = jest.fn(() => Promise.resolve('aaa'));
    const joiner = createJoiner();

    expect(joiner.isPending()).toBe(false);

    const promise = joiner.join(operationMock);

    expect(joiner.isPending()).toBe(true);
    expect(operationMock).toHaveBeenCalledTimes(1);

    await expect(promise).resolves.toBe('aaa');

    expect(operationMock).toHaveBeenCalledTimes(1);
  });

  test('joins the pending operation', async () => {
    const joiner = createJoiner();

    joiner.join(() => Promise.resolve('aaa'));

    const promise = joiner.join(() => Promise.resolve('bbb'));

    await expect(promise).resolves.toBe('aaa');
  });
});
