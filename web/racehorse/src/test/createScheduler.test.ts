import { expect, test, vi } from 'vitest';
import { createScheduler } from '../main/index.js';
import { noop } from '../main/utils.js';

test('invokes the action', async () => {
  const actionMock = vi.fn(() => Promise.resolve('aaa'));
  const scheduler = createScheduler();

  expect(scheduler.isPending()).toBe(false);

  const promise = scheduler.schedule(actionMock);

  expect(scheduler.isPending()).toBe(true);
  expect(actionMock).toHaveBeenCalledTimes(0);

  await expect(promise).resolves.toBe('aaa');

  expect(actionMock).toHaveBeenCalledTimes(1);
});

test('invokes actions consequently', async () => {
  let action1Completed = false;

  const action1Mock = vi.fn(() =>
    Promise.resolve().then(() => {
      action1Completed = true;
    })
  );
  const action2Mock = vi.fn(() => {
    expect(action1Completed).toBe(true);
    return Promise.resolve('aaa');
  });

  const scheduler = createScheduler();

  scheduler.schedule(action1Mock);

  await expect(scheduler.schedule(action2Mock)).resolves.toBe('aaa');

  expect(action2Mock).toHaveBeenCalledTimes(1);
  expect(scheduler.isPending()).toBe(false);
});

test('result from the previous action is not visible to the next action', async () => {
  const actionMock = vi.fn();

  const scheduler = createScheduler();

  scheduler.schedule(() => Promise.resolve('aaa'));

  await scheduler.schedule(actionMock);

  expect(actionMock).toHaveBeenCalledTimes(1);
  expect(actionMock.mock.calls[0]).toStrictEqual([expect.any(AbortSignal)]);
});

test('ignores the rejection of the preceding action', async () => {
  const actionMock = vi.fn();

  const scheduler = createScheduler();

  scheduler.schedule(() => Promise.reject('aaa')).catch(noop);

  await scheduler.schedule(actionMock);

  expect(actionMock).toHaveBeenCalledTimes(1);
  expect(actionMock.mock.calls[0]).toStrictEqual([expect.any(AbortSignal)]);
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
