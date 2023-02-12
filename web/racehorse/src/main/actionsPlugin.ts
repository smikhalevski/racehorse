import { Plugin } from './shared-types';

export interface ActionsMixin {
  /**
   * Opens a URL in an external application.
   *
   * @param url The URL to open.
   * @returns `true` if external application was opened, or `false` otherwise.
   */
  openUrl(url: string): Promise<boolean>;
}

/**
 * Launches activities for various intents.
 */
export const actionsPlugin: Plugin<ActionsMixin> = eventBridge => {
  eventBridge.openUrl = url => {
    return eventBridge.request({ type: 'org.racehorse.OpenUrlRequestEvent', url }).then(event => event.opened);
  };
};
