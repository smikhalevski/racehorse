import { Animation, AnimationHandler } from './types.js';

/**
 * Runs the animation frames and triggers corresponding handler callbacks.
 *
 * @param animation The animation to run.
 * @param handler The handler that is triggered when the animation state changes.
 * @param signal The signal that cancels the animation.
 * @template T The animation to run.
 */
export function runAnimation<T extends Partial<Animation>>(
  animation: T,
  handler: Partial<AnimationHandler<T>> | AnimationHandler<T>['onProgress'],
  signal?: AbortSignal
): void {
  handler = typeof handler === 'function' ? { onProgress: handler } : handler;

  let runTime = Date.now();
  let frameHandle = 0;
  let timer: number;

  const { duration = 0, startTime = runTime, easing } = animation;

  const frameRequestCallback = () => {
    const time = runTime - startTime + Date.now();

    const percent = duration > 0 ? Math.min((time - startTime) / duration, 1) : 1;

    handler.onProgress?.(animation, easing === undefined ? percent : easing(percent), percent);

    if (percent === 1) {
      handler.onEnd?.(animation);
      frameHandle = 0;
      return;
    }

    frameHandle = requestAnimationFrame(frameRequestCallback);
  };

  if (runTime < startTime) {
    timer = setTimeout(() => {
      // Defer the animation run
      runTime = Date.now();

      handler.onStart?.(animation);

      frameRequestCallback();
    }, startTime - runTime);
  } else {
    handler.onStart?.(animation);

    frameRequestCallback();
  }

  signal?.addEventListener('abort', () => {
    clearTimeout(timer);

    if (frameHandle === 0) {
      return;
    }
    cancelAnimationFrame(frameHandle);
    frameHandle = 0;
    handler.onAbort?.(animation);
  });
}
