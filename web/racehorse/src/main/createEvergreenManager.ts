import { EventBridge } from './createEventBridge';

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

export type UpdateMode = 'mandatory' | 'optional' | 'postponed';

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
   */
  applyUpdate(): Promise<string | null>;

  /**
   * The new update download has started.
   */
  subscribe(type: 'started', listener: (payload: { updateMode: UpdateMode }) => void): () => void;

  /**
   * Failed to download an update.
   */
  subscribe(type: 'failed', listener: (payload: { updateMode: UpdateMode }) => void): () => void;

  /**
   * An update was successfully downloaded and ready to be applied.
   */
  subscribe(
    type: 'ready',
    listener: (payload: {
      /**
       * The version of the update bundle that is ready to be applied.
       */
      version: string;
    }) => void
  ): () => void;

  /**
   * Progress of a pending update download.
   */
  subscribe(
    type: 'progress',
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
  ): () => void;
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
      eventBridge.request({ type: 'org.racehorse.evergreen.GetMasterVersionEvent' }).payload.version,

    getUpdateStatus: () => eventBridge.request({ type: 'org.racehorse.evergreen.GetUpdateStatusEvent' }).payload.status,

    applyUpdate: () =>
      (updatePromise ||= eventBridge.requestAsync({ type: 'org.racehorse.evergreen.ApplyUpdateEvent' }).then(event => {
        updatePromise = undefined;
        return event.payload.version;
      })),

    subscribe: (type, listener) =>
      eventBridge.subscribe(
        {
          started: 'org.racehorse.UpdateStartedEvent',
          failed: 'org.racehorse.UpdateFailedEvent',
          ready: 'org.racehorse.UpdateReadyEvent',
          progress: 'org.racehorse.UpdateProgressEvent',
        }[type],
        listener
      ),
  };
}
