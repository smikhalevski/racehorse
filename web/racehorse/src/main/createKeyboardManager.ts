import { EventBridge } from './createEventBridge';
import { Unsubscribe } from './types';

export interface KeyboardStatus {
  height: number;
  isVisible: boolean;
}

export interface KeyboardManager {
  /**
   * Returns the status of the software keyboard.
   */
  getKeyboardStatus(): KeyboardStatus;

  /**
   * Shows the keyboard.
   */
  showKeyboard(): void;

  /**
   * Hides the keyboard.
   *
   * @returns `true` if the keyboard was actually hidden, or `false` otherwise.
   */
  hideKeyboard(): boolean;

  /**
   * Subscribes a listener to software keyboard status changes.
   */
  subscribe(listener: (status: KeyboardStatus) => void): Unsubscribe;
}

/**
 * Manages keyboard visibility and provides its status updates.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createKeyboardManager(eventBridge: EventBridge): KeyboardManager {
  return {
    getKeyboardStatus: () => eventBridge.request({ type: 'org.racehorse.GetKeyboardStatusEvent' }).payload.status,

    showKeyboard() {
      eventBridge.request({ type: 'org.racehorse.ShowKeyboardEvent' });
    },

    hideKeyboard: () => eventBridge.request({ type: 'org.racehorse.HideKeyboardEvent' }).payload.isHidden,

    subscribe: listener =>
      eventBridge.subscribe('org.racehorse.KeyboardStatusChangedEvent', payload => {
        listener(payload.status);
      }),
  };
}
