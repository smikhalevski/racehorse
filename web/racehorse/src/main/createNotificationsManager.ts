import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface NotificationsManager {
  /**
   * Returns whether notifications are enabled in Settings app.
   */
  areNotificationsEnabled(): boolean;
}

/**
 * @param eventBridge The underlying event bridge.
 */
export function createNotificationsManager(eventBridge: EventBridge): NotificationsManager {
  return {
    areNotificationsEnabled: () =>
      ensureEvent(eventBridge.requestSync({ type: 'org.racehorse.AreNotificationsEnabledEvent' })).payload.isEnabled,
  };
}
