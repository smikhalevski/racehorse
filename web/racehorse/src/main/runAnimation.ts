import { Animation, AnimationHandler } from './types';

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

  const runTime = Date.now();

  const { duration = 0, startTime = runTime, easing } = animation;

  let frameHandle = 0;
  let timer: NodeJS.Timeout;

  const frameRequestCallback = () => {
    const time = Date.now() + runTime - startTime;

    const percent = Math.min((time - startTime) / duration, 1);

    handler.onProgress?.(animation, easing === undefined ? percent : easing(percent), percent);

    if (percent === 1) {
      handler.onEnd?.(animation);
      frameHandle = 0;
      return;
    }

    frameHandle = requestAnimationFrame(frameRequestCallback);
  };

  const startAnimation = () => {
    handler.onStart?.(animation);

    frameRequestCallback();
  };

  if (runTime < startTime) {
    timer = setTimeout(startAnimation, startTime - runTime);
  } else {
    startAnimation();
  }

  signal?.addEventListener('abort', () => {
    clearTimeout(timer);

    if (frameHandle === 0) {
      return;
    }
    frameHandle = 0;
    cancelAnimationFrame(frameHandle);
    handler.onAbort?.(animation);
  });
}
