import { EventBridge } from './createEventBridge';

/**
 * Device network monitoring.
 */
export interface NetworkManager {
  /**
   * Returns the current online status or `undefined` if not yet known.
   */
  isOnline(): boolean | undefined;

  /**
   * Subscribes to online status changes.
   *
   * @param listener The callback that is invoked if online status is changed.
   * @returns The callback to unsubscribe the listener.
   */
  subscribe(listener: (online: boolean) => void): () => void;
}

/**
 * Creates the new {@linkcode NetworkManager} instance.
 *
 * @param eventBridge The event bridge to use for communication with Android device.
 */
export function createNetworkManager(eventBridge: EventBridge): NetworkManager {
  let online: boolean | undefined;

  const promise = eventBridge
    .request({ type: 'org.racehorse.IsOnlineRequestEvent' })
    .then(event => (online = event.online));

  return {
    isOnline() {
      return online;
    },

    subscribe(listener) {
      let subscribed = true;

      promise.then(online => {
        if (subscribed) {
          listener(online);
        }
      });

      const unsubscribe = eventBridge.subscribe(event => {
        if (event.type === 'org.racehorse.OnlineStatusChangedAlertEvent') {
          listener(event.online);
        }
      });

      return () => {
        subscribed = false;
        unsubscribe();
      };
    },
  };
}
