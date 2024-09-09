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
    const unsubscribeBeforeChanged = manager.subscribe('beforeChanged', status => {
      if (!status.isVisible) {
        setKeyboardStatus(status);
      }
    });

    const unsubscribeChanged = manager.subscribe('changed', status => {
      if (status.isVisible) {
        setKeyboardStatus(status);
      }
    });

    return () => {
      unsubscribeBeforeChanged();
      unsubscribeChanged();
    };
  }, [manager]);

  return keyboardStatus;
}
