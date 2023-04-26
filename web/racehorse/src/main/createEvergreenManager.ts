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
  getMasterVersion(): Promise<string | null>;

  /**
   * Get the version of the update that would be applied on the next app restart.
   */
  getUpdateStatus(): Promise<UpdateStatus | null>;

  /**
   * Applies the available update bundle and returns its version, or returns `null` if there's no update bundle.
   */
  applyUpdate(): Promise<string | null>;
}

/**
 * Handles background updates.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createEvergreenManager(eventBridge: EventBridge): EvergreenManager {
  let updatePromise: Promise<string> | undefined;

  return {
    getMasterVersion: () =>
      eventBridge
        .request({ type: 'org.racehorse.evergreen.GetMasterVersionEvent' })
        .then(event => ensureEvent(event).version),

    getUpdateStatus: () =>
      eventBridge
        .request({ type: 'org.racehorse.evergreen.GetUpdateStatusEvent' })
        .then(event => ensureEvent(event).status),

    applyUpdate: () =>
      (updatePromise ||= eventBridge.request({ type: 'org.racehorse.evergreen.ApplyUpdateEvent' }).then(event => {
        updatePromise = undefined;
        return ensureEvent(event).version;
      })),
  };
}
