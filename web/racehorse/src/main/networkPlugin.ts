import { Plugin } from './shared-types';

export interface NetworkMixin {
  /**
   * Returns the current online status or `undefined` if not yet known.
   */
  readonly online: boolean | undefined;
}

/**
 * Device network monitoring.
 */
export const networkPlugin: Plugin<NetworkMixin> = (eventBridge, listener) => {
  let online: boolean | undefined;

  Object.defineProperty(eventBridge, 'online', {
    enumerable: true,
    get: () => online,
  });

  eventBridge.request({ type: 'org.racehorse.IsOnlineRequestEvent' }).then(event => {
    online = event.online;
    listener();
  });

  return eventBridge.subscribeToAlerts(event => {
    if (event.type === 'org.racehorse.OnlineStatusChangedAlertEvent') {
      online = event.online;
      listener();
    }
  });
};
