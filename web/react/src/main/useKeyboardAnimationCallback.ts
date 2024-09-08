import { useEffect } from 'react';
import { useKeyboardManager } from './managers';

export function useKeyboardAnimationCallback(callback: (height: number) => void): void {
  const manager = useKeyboardManager();

  useEffect(() => {
    let handle = 0;

    const unsubscribe = manager.subscribe(
      'beforeChange',
      (status, height, animationStartTimestamp, animationDuration, easing) => {
        const startTime = Date.now();

        cancelAnimationFrame(handle);

        const frameRequestCallback = () => {
          const time = Date.now() + startTime - animationStartTimestamp;

          const t = (time - startTime) / animationDuration;

          callback((status.isShown ? easing(t) : 1 - easing(t)) * height);

          if (t < 1) {
            handle = requestAnimationFrame(frameRequestCallback);
          }
        };

        frameRequestCallback();
      }
    );

    return () => {
      cancelAnimationFrame(handle);
      unsubscribe();
    };
  }, [manager]);
}
