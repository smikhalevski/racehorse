import { useEffect } from 'react';
import { useKeyboardManager } from './managers';

export function useKeyboardAnimationCallback(callback: (height: number) => void): void {
  const manager = useKeyboardManager();

  useEffect(() => {
    let handle = 0;

    const unsubscribe = manager.subscribe('beforeChange', (status, height, animationDuration, easing) => {
      let startTime = -1;

      cancelAnimationFrame(handle);

      const frameRequestCallback: FrameRequestCallback = time => {
        if (startTime === -1) {
          startTime = time;
        }

        const t = (time - startTime) / animationDuration;

        callback((status.isShown ? easing(t) : 1 - easing(t)) * height);

        if (t < 1) {
          handle = requestAnimationFrame(frameRequestCallback);
        }
      };

      requestAnimationFrame(frameRequestCallback);
    });

    return () => {
      cancelAnimationFrame(handle);
      unsubscribe();
    };
  }, [manager]);
}
