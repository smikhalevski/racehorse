import { useEffect, useState } from 'react';
import { KeyboardStatus } from 'racehorse';
import { useKeyboardManager } from './managers';

/**
 * Returns the current keyboard status and re-renders the component if it changes.
 */
export function useKeyboardStatus(): KeyboardStatus {
  const manager = useKeyboardManager();

  const [keyboardStatus, setKeyboardStatus] = useState(manager.getKeyboardStatus);

  useEffect(() => {
    const unsubscribeBefore = manager.subscribe('beforeChange', status => {
      if (!status.isShown) {
        setKeyboardStatus(status);
      }
    });

    const unsubscribeAfter = manager.subscribe('afterChange', status => {
      if (status.isShown) {
        setKeyboardStatus(status);
      }
    });

    return () => {
      unsubscribeBefore();
      unsubscribeAfter();
    };
  }, [manager]);

  return keyboardStatus;
}
