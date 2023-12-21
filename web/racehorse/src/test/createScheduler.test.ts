import { createScheduler } from '../main';

describe('createScheduler', () => {
  test('invokes the operation', async () => {
    const operationMock = jest.fn(() => Promise.resolve('aaa'));
    const scheduler = createScheduler();

    expect(scheduler.isPending()).toBe(false);

    const promise = scheduler.schedule(operationMock);

    expect(scheduler.isPending()).toBe(true);
    expect(operationMock).toHaveBeenCalledTimes(0);

    await expect(promise).resolves.toBe('aaa');

    expect(operationMock).toHaveBeenCalledTimes(1);
  });

  test('invokes operations consequently', async () => {
    let operation1Completed = false;

    const operation1Mock = jest.fn(() =>
      Promise.resolve().then(() => {
        operation1Completed = true;
      })
    );
    const operation2Mock = jest.fn(() => {
      expect(operation1Completed).toBe(true);
      return Promise.resolve('aaa');
    });

    const scheduler = createScheduler();

    scheduler.schedule(operation1Mock);

    await expect(scheduler.schedule(operation2Mock)).resolves.toBe('aaa');

    expect(operation2Mock).toHaveBeenCalledTimes(1);
    expect(scheduler.isPending()).toBe(false);
  });

  test('result from the previous operation is not visible to the next operation', async () => {
    const operationMock = jest.fn();

    const scheduler = createScheduler();

    scheduler.schedule(() => Promise.resolve('aaa'));

    await scheduler.schedule(operationMock);

    expect(operationMock).toHaveBeenCalledTimes(1);
    expect(operationMock.mock.calls[0]).toStrictEqual([undefined]);
  });

  test('ignores the rejection of the preceding operation', async () => {
    const operationMock = jest.fn();

    const scheduler = createScheduler();

    scheduler.schedule(() => Promise.reject('aaa'));

    await scheduler.schedule(operationMock);

    expect(operationMock).toHaveBeenCalledTimes(1);
    expect(operationMock.mock.calls[0]).toStrictEqual([undefined]);
  });

  test('pending status is preserved', async () => {
    const scheduler = createScheduler();

    expect(scheduler.isPending()).toBe(false);

    const promise1 = scheduler.schedule(() => Promise.resolve('aaa'));
    const promise2 = scheduler.schedule(() => Promise.resolve('bbb'));

    await promise1;

    expect(scheduler.isPending()).toBe(true);

    await promise2;

    expect(scheduler.isPending()).toBe(false);
  });
});
