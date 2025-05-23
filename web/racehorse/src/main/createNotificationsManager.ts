import { EventBridge } from './createEventBridge.js';

export interface NotificationsManager {
  /**
   * Returns whether notifications are enabled in Settings app.
   */
  areNotificationsEnabled(): boolean;
}

/**
 * Manages system notifications.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createNotificationsManager(eventBridge: EventBridge): NotificationsManager {
  return {
    areNotificationsEnabled: () =>
      eventBridge.request({ type: 'org.racehorse.AreNotificationsEnabledEvent' }).payload.isEnabled,
  };
}
