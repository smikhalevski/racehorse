import { createContext, useContext, useEffect, useState } from 'react';
import { activityManager, ActivityState } from 'racehorse';

/**
 * Provides the `ActivityManager` instance to underlying components.
 */
export const ActivityManagerContext = createContext(activityManager);

ActivityManagerContext.displayName = 'ActivityManagerContext';

/**
 * Returns the current activity state and re-renders the component if it changes.
 */
export function useActivityState(): ActivityState {
  const manager = useContext(ActivityManagerContext);

  const [state, setState] = useState(manager.getActivityState);

  useEffect(() => manager.subscribe(setState), [manager]);

  return state;
}
