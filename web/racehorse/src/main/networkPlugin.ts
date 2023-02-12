import { Plugin } from './shared-types';

export interface NetworkMixin {
  /**
   * Returns the current online status or `undefined` if not yet known.
   */
  online: boolean | undefined;
}

/**
 * Device network monitoring.
 */
export const networkPlugin: Plugin<NetworkMixin> = (eventBridge, listener) => {
  eventBridge.online = eventBridge.requestSync({ type: 'org.racehorse.IsOnlineRequestEvent' })?.online;

  eventBridge.watchForAlerts(event => {
    if (event.type === 'org.racehorse.OnlineStatusChangedAlertEvent') {
      eventBridge.online = event.online;
      listener();
    }
  });
};
