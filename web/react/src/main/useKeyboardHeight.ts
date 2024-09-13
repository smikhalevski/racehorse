import { useKeyboardManager } from './managers';
import { useEffect, useState } from 'react';

/**
 * Returns the current keyboard height and re-renders the component when it changes.
 *
 * If keyboard is shown, then a component is re-rendered after the keyboard animation completes. If keyboard is hidden,
 * then a component is re-rendered immediately.
 */
export function useKeyboardHeight(): number {
  const manager = useKeyboardManager();
  const [keyboardHeight, setKeyboardHeight] = useState(manager.getKeyboardHeight);

  useEffect(() => {
    let timer: NodeJS.Timeout;

    setKeyboardHeight(manager.getKeyboardHeight());

    const unsubscribe = manager.subscribe(animation => {
      clearTimeout(timer);

      const duration = animation.startTime - Date.now() + animation.duration;

      if (animation.startValue > animation.endValue || duration === 0) {
        setKeyboardHeight(animation.endValue);
      } else {
        timer = setTimeout(setKeyboardHeight, duration, animation.endValue);
      }
    });

    return () => {
      clearTimeout(timer);
      unsubscribe();
    };
  }, [manager]);

  return keyboardHeight;
}
