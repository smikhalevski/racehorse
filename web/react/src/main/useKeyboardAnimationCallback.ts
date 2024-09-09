import { useEffect } from 'react';
import { useKeyboardManager } from './managers';
import { KeyboardStatus } from 'racehorse';

/**
 * Invokes a callback with the current keyboard height when keyboard animation takes place.
 */
export function useKeyboardAnimationCallback(
  callback: (keyboardHeight: number, keyboardStatus: KeyboardStatus) => void
): void {
  const manager = useKeyboardManager();

  useEffect(() => {
    let handle = 0;

    const unsubscribe = manager.subscribe('beforeChanged', (status, animation) => {
      const startTimestamp = Date.now();

      cancelAnimationFrame(handle);

      const frameRequestCallback = () => {
        const timestamp = Date.now() + startTimestamp - animation.startTimestamp;

        const t = (timestamp - animation.startTimestamp) / animation.duration;

        callback(
          animation.endValue > animation.startValue
            ? animation.endValue * animation.easing(t)
            : animation.startValue * (1 - animation.easing(t)),
          status
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
