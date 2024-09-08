import { EventBridge } from './createEventBridge';
import { Unsubscribe } from './types';
import { noop } from './utils';

export interface KeyboardStatus {
  height: number;
  isShown: boolean;
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
  subscribe(
    eventType: 'beforeChange',
    listener: (status: KeyboardStatus, height: number, animationDuration: number, easing: (t: number) => number) => void
  ): Unsubscribe;

  /**
   * Subscribes a listener to software keyboard status changes.
   */
  subscribe(eventType: 'afterChange', listener: (status: KeyboardStatus) => void): Unsubscribe;
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

    subscribe: (eventType, listener) => {
      if (eventType === 'beforeChange') {
        return eventBridge.subscribe('org.racehorse.BeforeKeyboardStatusChangeEvent', payload => {
          listener(payload.status, payload.height, payload.animationDuration, createEasing(payload.ordinates));
        });
      }

      if (eventType === 'afterChange') {
        return eventBridge.subscribe('org.racehorse.AfterKeyboardStatusChangeEvent', payload => {
          (listener as Function)(payload.status);
        });
      }

      return noop;
    },
  };
}

export function createEasing(ordinates: number[]): (t: number) => number {
  if (ordinates.length === 0) {
    return () => 1;
  }

  return t => {
    if (t <= 0) {
      return 0;
    }
    if (t >= 1) {
      return 1;
    }

    // lerp
    const x = t * (ordinates.length - 1);

    const startX = x | 0;
    const startY = ordinates[startX];

    return startY + (ordinates[startX + 1] - startY) /*deltaY*/ * (x - startX);
  };
}
