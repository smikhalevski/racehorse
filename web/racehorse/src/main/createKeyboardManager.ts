import { EventBridge } from './createEventBridge';

export interface KeyboardStatus {
  height: number;
  isVisible: boolean;
}

export interface KeyboardManager {
  /**
   * Returns the status of the software keyboard.
   */
  getStatus(): KeyboardStatus;

  /**
   * Subscribes a listener to software keyboard status changes.
   */
  subscribe(listener: (status: KeyboardStatus) => void): () => void;
}

export function createKeyboardManager(eventBridge: EventBridge): KeyboardManager {
  return {
    getStatus: () => eventBridge.request({ type: 'org.racehorse.GetKeyboardStatusEvent' }).payload.status,

    subscribe: listener =>
      eventBridge.subscribe('org.racehorse.KeyboardStatusChangedEvent', payload => {
        listener(payload.status);
      }),
  };
}
