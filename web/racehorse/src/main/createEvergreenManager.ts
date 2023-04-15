import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface UpdateStatus {
  /**
   * The version of the update.
   */
  version: string;

  /**
   * `true` if the update is fully downloaded and ready to be applied.
   */
  isReady: boolean;
}

export interface EvergreenManager {
  /**
   * The current version of the app bundle.
   */
  getMasterVersion(): Promise<string>;

  /**
   * Get the version of the update that would be applied on the next app restart.
   */
  getUpdateStatus(): Promise<UpdateStatus | null>;
}

/**
 * Retrieves bundle versions and update status.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createEvergreenManager(eventBridge: EventBridge): EvergreenManager {
  return {
    getMasterVersion: () =>
      eventBridge
        .request({ type: 'org.racehorse.evergreen.GetMasterVersionRequestEvent' })
        .then(event => ensureEvent(event).version),

    getUpdateStatus: () =>
      eventBridge
        .request({ type: 'org.racehorse.evergreen.GetUpdateStatusRequestEvent' })
        .then(event => ensureEvent(event).status),
  };
}
