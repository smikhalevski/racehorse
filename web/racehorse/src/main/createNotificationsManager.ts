import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface NotificationsManager {
  /**
   * Returns whether notifications are enabled.
   */
  areNotificationsEnabled(): Promise<boolean>;
}

/**
 * @param eventBridge The underlying event bridge.
 */
export function createNotificationsManager(eventBridge: EventBridge): NotificationsManager {
  return {
    areNotificationsEnabled: () =>
      eventBridge
        .request({ type: 'org.racehorse.AreNotificationsEnabledRequestEvent' })
        .then(event => ensureEvent(event).enabled),
  };
}
