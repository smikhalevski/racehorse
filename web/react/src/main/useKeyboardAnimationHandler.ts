import { useEffect, useRef } from 'react';
import { useKeyboardManager } from './managers';
import { Animation } from 'racehorse';

export interface KeyboardAnimationHandler {
  /**
   * Triggered right before the animation is started.
   *
   * @param animation The animation that has started.
   */
  onStart?(animation: Animation): void;

  /**
   * Triggered when a keyboard height has changed during animation.
   *
   * @param animation The pending animation.
   * @param keyboardHeight The current keyboard height.
   * @param percent The completed animation percentage.
   */
  onProgress?(animation: Animation, keyboardHeight: number, percent: number): void;

  /**
   * Triggered right after the animation has ended.
   *
   * @param animation The animation that has ended.
   */
  onEnd?(animation: Animation): void;
}

/**
 * Invokes callbacks with the keyboard animation takes place.
 */
export function useKeyboardAnimationHandler(
  handler: KeyboardAnimationHandler | NonNullable<KeyboardAnimationHandler['onProgress']>
): void {
  const manager = useKeyboardManager();

  handler = typeof handler === 'function' ? { onProgress: handler } : handler;

  const handlerRef = useRef(handler);

  handlerRef.current = handler;

  useEffect(() => {
    let handle = 0;

    const unsubscribe = manager.subscribe('beforeToggled', animation => {
      const startTime = Date.now();

      cancelAnimationFrame(handle);

      const frameRequestCallback = () => {
        const time = Date.now() + startTime - animation.startTime;

        const t = (time - animation.startTime) / animation.duration;

        const percent = animation.easing(t);

        handlerRef.current.onProgress?.(
          animation,
          animation.endValue > animation.startValue
            ? animation.endValue * percent
            : animation.startValue * (1 - percent),
          percent
        );

        if (t < 1) {
          handle = requestAnimationFrame(frameRequestCallback);
          return;
        }

        handlerRef.current.onEnd?.(animation);
      };

      handlerRef.current.onStart?.(animation);

      frameRequestCallback();
    });

    return () => {
      cancelAnimationFrame(handle);
      unsubscribe();
    };
  }, [manager]);
}
