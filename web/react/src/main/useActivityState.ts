import { useEffect, useState } from 'react';
import { ActivityState } from 'racehorse';
import { useActivityManager } from './managers.js';

/**
 * Returns the current activity state and re-renders the component if it changes.
 */
export function useActivityState(): ActivityState {
  const manager = useActivityManager();

  const [activityState, setActivityState] = useState(manager.getActivityState);

  useEffect(() => manager.subscribe(setActivityState), [manager]);

  return activityState;
}
