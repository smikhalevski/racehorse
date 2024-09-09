import { EventBridge } from './createEventBridge';
import { Unsubscribe } from './types';
import { noop } from './utils';

export interface KeyboardStatus {
  /**
   * The visible height of the software keyboard.
   */
  height: number;

  /**
   * `true` if the software keyboard is visible.
   */
  isVisible: boolean;
}

/**
 * An animation that takes place on the native side.
 */
export interface Animation {
  /**
   * A value from which an animation starts.
   */
  startValue: number;

  /**
   * A value at which an animation ends.
   */
  endValue: number;

  /**
   * An animation duration in milliseconds.
   */
  duration: number;

  /**
   * An easing function that converts `t` ∈ [0, 1] to `y` ∈ [0, 1].
   */
  easing: (t: number) => number;

  /**
   * An easing curve described as an array of at least two ordinate values (y ∈ [0, 1]) that correspond to
   * an equidistant abscissa values (y).
   */
  easingValues: number[];

  /**
   * A timestamp when an animation has started.
   */
  startTimestamp: number;
}

export interface KeyboardManager {
  /**
   * Returns the status of the software keyboard.
   */
  getKeyboardStatus(): KeyboardStatus;

  /**
   * Shows the software keyboard.
   */
  showKeyboard(): void;

  /**
   * Hides the software keyboard.
   */
  hideKeyboard(): void;

  /**
   * Subscribes a listener to the start of the software keyboard animation.
   */
  subscribe(eventType: 'beforeChanged', listener: (status: KeyboardStatus, animation: Animation) => void): Unsubscribe;

  /**
   * Subscribes a listener to the software keyboard status changes. Changes are published after an animation has finished.
   */
  subscribe(eventType: 'changed', listener: (status: KeyboardStatus) => void): Unsubscribe;
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

    hideKeyboard() {
      eventBridge.request({ type: 'org.racehorse.HideKeyboardEvent' });
    },

    subscribe: (eventType, listener) => {
      if (eventType === 'beforeChanged') {
        return eventBridge.subscribe('org.racehorse.BeforeKeyboardStatusChangedEvent', payload => {
          payload.animation.easing = createEasingFunction(payload.animation.easingValues);

          listener(payload.status, payload.animation);
        });
      }

      if (eventType === 'changed') {
        return eventBridge.subscribe('org.racehorse.KeyboardStatusChangedEvent', payload => {
          (listener as Function)(payload.status);
        });
      }

      return noop;
    },
  };
}

/**
 * Creates an easing function that converts `t` ∈ [0, 1] to `y` ∈ [0, 1].
 *
 * @see [<easing-function>](https://developer.mozilla.org/en-US/docs/Web/CSS/easing-function)
 */
export function createEasingFunction(easingValues: number[]): (t: number) => number {
  return t => {
    if (t <= 0) {
      return 0;
    }
    if (t >= 1) {
      return 1;
    }

    const x = t * (easingValues.length - 1);

    const startX = x | 0;
    const startY = easingValues[startX];

    return startY + (easingValues[startX + 1] - startY) * (x - startX);
  };
}
