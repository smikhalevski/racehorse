/**
 * @vitest-environment jsdom
 */

import { beforeEach, expect, test, vi } from 'vitest';
import { Animation, AnimationHandler, runAnimation } from '../main/index.js';

vi.useFakeTimers();

let handlerMock: AnimationHandler<any>;

beforeEach(() => {
  handlerMock = {
    onAbort: vi.fn(),
    onEnd: vi.fn(),
    onProgress: vi.fn(),
    onStart: vi.fn(),
  };
});

test('starts the animation instantly', () => {
  const animation: Partial<Animation> = { duration: 100 };

  runAnimation(animation, handlerMock);

  expect(handlerMock.onStart).toHaveBeenCalledTimes(1);
  expect(handlerMock.onStart).toHaveBeenNthCalledWith(1, animation);

  expect(handlerMock.onProgress).toHaveBeenCalledTimes(1);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(1, animation, 0, 0);

  expect(handlerMock.onEnd).not.toHaveBeenCalled();
  expect(handlerMock.onAbort).not.toHaveBeenCalled();
});

test('defers the animation start', () => {
  const animation: Partial<Animation> = { duration: 100, startTime: Date.now() + 1000 };

  runAnimation(animation, handlerMock);

  expect(handlerMock.onStart).not.toHaveBeenCalled();
  expect(handlerMock.onProgress).not.toHaveBeenCalled();
  expect(handlerMock.onEnd).not.toHaveBeenCalled();
  expect(handlerMock.onAbort).not.toHaveBeenCalled();

  vi.advanceTimersByTime(500);

  expect(handlerMock.onStart).not.toHaveBeenCalled();
  expect(handlerMock.onProgress).not.toHaveBeenCalled();

  vi.advanceTimersByTime(500);

  expect(handlerMock.onStart).toHaveBeenCalledTimes(1);
  expect(handlerMock.onStart).toHaveBeenNthCalledWith(1, animation);

  expect(handlerMock.onProgress).toHaveBeenCalledTimes(1);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(1, animation, 0, 0);
});

test('cancels the deferred animation start', () => {
  const abortController = new AbortController();
  const animation: Partial<Animation> = { duration: 100, startTime: Date.now() + 1000 };

  runAnimation(animation, handlerMock, abortController.signal);

  vi.advanceTimersByTime(500);

  abortController.abort();

  vi.advanceTimersByTime(1000);

  expect(handlerMock.onStart).not.toHaveBeenCalled();
  expect(handlerMock.onProgress).not.toHaveBeenCalled();
  expect(handlerMock.onEnd).not.toHaveBeenCalled();
  expect(handlerMock.onAbort).not.toHaveBeenCalled();
});

test('starts the animation with startTime in the past', () => {
  const animation: Partial<Animation> = { duration: 5000, startTime: Date.now() - 1000 };

  runAnimation(animation, handlerMock);

  expect(handlerMock.onStart).toHaveBeenCalledTimes(1);
  expect(handlerMock.onStart).toHaveBeenNthCalledWith(1, animation);

  expect(handlerMock.onProgress).toHaveBeenCalledTimes(1);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(1, animation, 0.4, 0.4);

  expect(handlerMock.onEnd).not.toHaveBeenCalled();
  expect(handlerMock.onAbort).not.toHaveBeenCalled();
});

test('runs the animation', () => {
  const animation: Partial<Animation> = { duration: 1000 };

  runAnimation(animation, handlerMock);

  vi.advanceTimersByTime(1100);

  expect(handlerMock.onStart).toHaveBeenCalledTimes(1);
  expect(handlerMock.onStart).toHaveBeenNthCalledWith(1, animation);

  expect(handlerMock.onProgress).toHaveBeenCalledTimes(64);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(1, animation, 0, 0);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(32, animation, 0.492, 0.492);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(64, animation, 1, 1);

  expect(handlerMock.onEnd).toHaveBeenCalledTimes(1);
});

test('cancels the running animation', () => {
  const abortController = new AbortController();
  const animation: Partial<Animation> = { duration: 1000 };

  runAnimation(animation, handlerMock, abortController.signal);

  vi.advanceTimersByTime(500);

  abortController.abort();

  expect(handlerMock.onStart).toHaveBeenCalledTimes(1);
  expect(handlerMock.onStart).toHaveBeenNthCalledWith(1, animation);

  expect(handlerMock.onProgress).toHaveBeenCalledTimes(32);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(1, animation, 0, 0);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(32, animation, 0.496, 0.496);

  expect(handlerMock.onEnd).not.toHaveBeenCalled();
  expect(handlerMock.onAbort).toHaveBeenCalledTimes(1);

  vi.advanceTimersByTime(500);

  expect(handlerMock.onProgress).toHaveBeenCalledTimes(32);
  expect(handlerMock.onEnd).not.toHaveBeenCalled();
  expect(handlerMock.onAbort).toHaveBeenCalledTimes(1);
});

test('successfully ends the animation', () => {
  const animation: Partial<Animation> = { duration: 1000 };

  runAnimation(animation, handlerMock);

  vi.advanceTimersByTime(2000);

  expect(handlerMock.onStart).toHaveBeenCalledTimes(1);
  expect(handlerMock.onStart).toHaveBeenNthCalledWith(1, animation);

  expect(handlerMock.onProgress).toHaveBeenCalledTimes(64);

  expect(handlerMock.onEnd).toHaveBeenCalledTimes(1);
  expect(handlerMock.onEnd).toHaveBeenNthCalledWith(1, animation);
  expect(handlerMock.onAbort).not.toHaveBeenCalled();

  vi.advanceTimersByTime(1000);

  expect(handlerMock.onProgress).toHaveBeenCalledTimes(64);
  expect(handlerMock.onEnd).toHaveBeenCalledTimes(1);
  expect(handlerMock.onAbort).not.toHaveBeenCalled();
});

test('uses callback as a handler', () => {
  const animation: Partial<Animation> = { duration: 1000 };
  const handlerMock = vi.fn();

  runAnimation(animation, handlerMock);

  vi.advanceTimersByTime(1100);

  expect(handlerMock).toHaveBeenCalledTimes(64);
  expect(handlerMock).toHaveBeenNthCalledWith(1, animation, 0, 0);
});

test('respects easing', () => {
  const animation: Partial<Animation> = { duration: 1000, easing: t => t * t };

  runAnimation(animation, handlerMock);

  vi.advanceTimersByTime(2000);

  expect(handlerMock.onProgress).toHaveBeenCalledTimes(65);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(1, animation, 0, 0);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(32, animation, 0.234256, 0.484);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(64, animation, 0.992016, 0.996);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(65, animation, 1, 1);
});

test('no animation duration by default', () => {
  const animation: Partial<Animation> = {};

  runAnimation(animation, handlerMock);

  expect(handlerMock.onStart).toHaveBeenCalledTimes(1);
  expect(handlerMock.onProgress).toHaveBeenCalledTimes(1);
  expect(handlerMock.onProgress).toHaveBeenNthCalledWith(1, animation, 1, 1);
  expect(handlerMock.onEnd).toHaveBeenCalledTimes(1);
});
