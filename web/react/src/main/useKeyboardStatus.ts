import { createContext, useContext, useEffect, useState } from 'react';
import { keyboardManager, KeyboardStatus } from 'racehorse';

/**
 * Provides the `KeyboardManager` instance to underlying components.
 */
export const KeyboardManagerContext = createContext(keyboardManager);

KeyboardManagerContext.displayName = 'KeyboardManagerContext';

/**
 * Returns the current keyboard status and re-renders the component if it changes.
 */
export function useKeyboardStatus(): KeyboardStatus {
  const manager = useContext(KeyboardManagerContext);

  const [status, setStatus] = useState(manager.getStatus);

  useEffect(() => manager.subscribe(setStatus), [manager]);

  return status;
}
