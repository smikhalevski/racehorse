import { EventBridge } from './createEventBridge';
import { Unsubscribe } from './types';

export interface BundleInfo {
  /**
   * The version of the bundle that is currently running.
   */
  masterVersion: string | null;

  /**
   * The version of the update.
   */
  updateVersion: string | null;
  isMasterReady: string;

  /**
   * `true` if the update is fully downloaded and ready to be applied.
   */
  isUpdateReady: string;

  /**
   * The directory where the master bundle is stored.
   */
  masterDir: string;

  /**
   * The directory where the update bundle is stored.
   */
  updateDir: string;
}

/**
 * @deprecated Use {@link BundleInfo} instead.
 */
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

export const UpdateMode = {
  MANDATORY: 'mandatory',
  OPTIONAL: 'optional',
  POSTPONED: 'postponed',
} as const;

export type UpdateMode = (typeof UpdateMode)[keyof typeof UpdateMode];

export interface EvergreenManager {
  /**
   * The current version of the app bundle.
   *
   * @deprecated Use {@link getBundleInfo} instead.
   */
  getMasterVersion(): string | null;

  /**
   * Get the version of the update that would be applied on the next app restart.
   *
   * @deprecated Use {@link getBundleInfo} instead.
   */
  getUpdateStatus(): UpdateStatus | null;

  /**
   * Get the info about the current bundle status.
   */
  getBundleInfo(): BundleInfo;

  /**
   * Applies the available update bundle and returns its version, or returns `null` if there's no update bundle.
   *
   * For changes to take effect you must reload the app.
   */
  applyUpdate(): Promise<string | null>;

  /**
   * The new update download has started.
   */
  subscribe(eventType: 'started', listener: (payload: { updateMode: UpdateMode }) => void): Unsubscribe;

  /**
   * Failed to download an update.
   */
  subscribe(eventType: 'failed', listener: (payload: { updateMode: UpdateMode }) => void): Unsubscribe;

  /**
   * An update was successfully downloaded and ready to be applied.
   */
  subscribe(
    eventType: 'ready',
    listener: (payload: {
      /**
       * The version of the update bundle that is ready to be applied.
       */
      version: string;
    }) => void
  ): Unsubscribe;

  /**
   * Progress of a pending update download.
   */
  subscribe(
    eventType: 'progress',
    listener: (payload: {
      /**
       * The length of downloaded content in bytes, or -1 if content length cannot be detected.
       */
      contentLength: number;
      /**
       * The number of bytes that are already downloaded.
       */
      readLength: number;
    }) => void
  ): Unsubscribe;
}

/**
 * Handles background updates.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createEvergreenManager(eventBridge: EventBridge): EvergreenManager {
  let promise: Promise<string | null> | undefined;

  return {
    getMasterVersion: () =>
      eventBridge.request({ type: 'org.racehorse.evergreen.GetMasterVersionEvent' }).payload.version,

    getUpdateStatus: () => eventBridge.request({ type: 'org.racehorse.evergreen.GetUpdateStatusEvent' }).payload.status,

    getBundleInfo: () => eventBridge.request({ type: 'org.racehorse.evergreen.GetBundleInfoEvent' }).payload,

    applyUpdate: () =>
      (promise ||= eventBridge.requestAsync({ type: 'org.racehorse.evergreen.ApplyUpdateEvent' }).then(
        event => {
          promise = undefined;
          return event.payload.version;
        },
        error => {
          promise = undefined;
          throw error;
        }
      )),

    subscribe: (eventType, listener) =>
      eventBridge.subscribe(
        {
          started: 'org.racehorse.UpdateStartedEvent',
          failed: 'org.racehorse.UpdateFailedEvent',
          ready: 'org.racehorse.UpdateReadyEvent',
          progress: 'org.racehorse.UpdateProgressEvent',
        }[eventType],
        listener
      ),
  };
}
