import { EventBridge } from './createEventBridge';
import { createJoiner } from './createJoiner';
import { Unsubscribe } from './types';

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
   */
  getMasterVersion(): string | null;

  /**
   * Get the version of the update that would be applied on the next app restart.
   */
  getUpdateStatus(): UpdateStatus | null;

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
  const updateJoiner = createJoiner<string | null>();

  return {
    getMasterVersion: () =>
      eventBridge.request({ type: 'org.racehorse.evergreen.GetMasterVersionEvent' }).payload.version,

    getUpdateStatus: () => eventBridge.request({ type: 'org.racehorse.evergreen.GetUpdateStatusEvent' }).payload.status,

    applyUpdate: () =>
      updateJoiner.join(() =>
        eventBridge
          .requestAsync({ type: 'org.racehorse.evergreen.ApplyUpdateEvent' })
          .then(event => event.payload.version)
      ),

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
