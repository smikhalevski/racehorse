import { EventBridge } from './createEventBridge';

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
      eventBridge.request({ type: 'org.racehorse.AreNotificationsEnabledEvent' }).payload.isEnabled,
  };
}
