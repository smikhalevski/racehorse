import { useEffect } from 'react';
import { useKeyboardManager } from './managers';
import { Animation, KeyboardStatus } from 'racehorse';

/**
 * @param height The current height of the keyboard.
 * @param percent The passed percentage of the animation duration.
 * @param status The status to which keyboard is animated to.
 * @param animation The animation that takes place.
 */
export type KeyboardAnimationCallback = (
  height: number,
  percent: number,
  status: KeyboardStatus,
  animation: Animation
) => void;

/**
 * Invokes a callback with the current keyboard height when keyboard animation takes place.
 */
export function useKeyboardAnimationCallback(callback: KeyboardAnimationCallback): void {
  const manager = useKeyboardManager();

  useEffect(() => {
    let handle = 0;

    const unsubscribe = manager.subscribe('beforeChanged', (status, animation) => {
      const startTimestamp = Date.now();

      cancelAnimationFrame(handle);

      const frameRequestCallback = () => {
        const timestamp = Date.now() + startTimestamp - animation.startTimestamp;

        const t = (timestamp - animation.startTimestamp) / animation.duration;

        const percent = animation.easing(t);

        callback(
          animation.endValue > animation.startValue
            ? animation.endValue * percent
            : animation.startValue * (1 - percent),
          percent,
          status,
          animation
        );

        if (t < 1) {
          handle = requestAnimationFrame(frameRequestCallback);
        }
      };

      frameRequestCallback();
    });

    return () => {
      cancelAnimationFrame(handle);
      unsubscribe();
    };
  }, [manager]);
}
