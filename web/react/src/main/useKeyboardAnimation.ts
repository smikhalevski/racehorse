import { useEffect } from 'react';
import { useKeyboardManager } from './managers';
import { TweenAnimation } from 'racehorse';

/**
 * Triggers a callback when the software keyboard animation is started.
 *
 * The signal provided to the callback is aborted if a new animation has started or if component unmounts.
 *
 * @param callback The callback that is triggered when the software keyboard animation is started.
 */
export function useKeyboardAnimation(callback: (animation: TweenAnimation, signal: AbortSignal) => void): void {
  const manager = useKeyboardManager();

  useEffect(() => {
    let abortController: AbortController | undefined;

    const unsubscribe = manager.subscribe(animation => {
      abortController?.abort();
      abortController = new AbortController();

      callback(animation, abortController.signal);
    });

    return () => {
      abortController?.abort();
      unsubscribe();
    };
  }, [manager]);
}
