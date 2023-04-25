import { EventBridge, WebActivityResult, WebIntent } from './types';
import { ensureEvent } from './utils';

export interface IntentsManager {
  /**
   * Start an activity for the intent.
   *
   * @returns The activity result.
   */
  startActivityForResult(intent: WebIntent): Promise<WebActivityResult<WebIntent | null> | null>;

  /**
   * Starts an activity for the intent.
   *
   * @returns `true` if activity has started, or `false` otherwise.
   */
  startActivity(intent: WebIntent): Promise<boolean>;

  /**
   * Opens a URI in an external application.
   *
   * @param uri The URI to open.
   * @param excludedPackageNames The array of package names that shouldn't be used to open the external application.
   * If used then the `android.permission.QUERY_ALL_PACKAGES` should be granted, otherwise no activity would be started.
   * @returns `true` if external application was opened, or `false` otherwise.
   */
  openApplication(uri: string, excludedPackageNames?: string[]): Promise<boolean>;
}

/**
 * Launches activities for various intents.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createIntentsManager(eventBridge: EventBridge): IntentsManager {
  return {
    startActivityForResult: intent =>
      eventBridge
        .request({ type: 'org.racehorse.StartActivityForResultRequestEvent', intent })
        .then(event => ensureEvent(event).result),

    startActivity: intent =>
      eventBridge
        .request({ type: 'org.racehorse.StartActivityRequestEvent', intent })
        .then(event => ensureEvent(event).isStarted),

    openApplication: uri =>
      eventBridge
        .request({ type: 'org.racehorse.OpenApplicationRequestEvent', uri })
        .then(event => ensureEvent(event).isOpened),
  };
}
