import { EventBridge } from './createEventBridge';
import { Unsubscribe } from './types';
import { noop } from './utils';

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
   * A timestamp when an animation has started.
   */
  startTime: number;
}

export interface KeyboardManager {
  /**
   * Returns the current height of the software keyboard.
   */
  getKeyboardHeight(): number;

  /**
   * Shows the software keyboard.
   */
  showKeyboard(): void;

  /**
   * Hides the software keyboard.
   */
  hideKeyboard(): void;

  /**
   * Subscribes a listener to the start of the software keyboard toggle animation.
   */
  subscribe(eventType: 'beforeToggled', listener: (animation: Animation) => void): Unsubscribe;

  /**
   * Subscribes a listener to the software keyboard toggle. Changes are published after an animation has finished.
   */
  subscribe(eventType: 'toggled', listener: (keyboardHeight: number) => void): Unsubscribe;
}

/**
 * Manages keyboard visibility and provides its status updates.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createKeyboardManager(eventBridge: EventBridge): KeyboardManager {
  return {
    getKeyboardHeight: () => eventBridge.request({ type: 'org.racehorse.GetKeyboardHeightEvent' }).payload.height,

    showKeyboard() {
      eventBridge.request({ type: 'org.racehorse.ShowKeyboardEvent' });
    },

    hideKeyboard() {
      eventBridge.request({ type: 'org.racehorse.HideKeyboardEvent' });
    },

    subscribe: (eventType, listener) => {
      if (eventType === 'beforeToggled') {
        return eventBridge.subscribe('org.racehorse.BeforeKeyboardToggledEvent', payload => {
          const animation = Object.assign({}, payload.animation);

          animation.easing = createEasingFunction(animation.easing);

          listener(animation);
        });
      }

      if (eventType === 'toggled') {
        return eventBridge.subscribe('org.racehorse.KeyboardToggledEvent', payload => {
          listener(payload.height);
        });
      }

      return noop;
    },
  };
}

/**
 * Creates an easing function that converts `t` ∈ [0, 1] to `y` ∈ [0, 1].
 *
 * @param ordinates An easing curve described as an array of at least two ordinate values (y ∈ [0, 1]) that correspond
 * to an equidistant abscissa values (x).
 * @see [<easing-function>](https://developer.mozilla.org/en-US/docs/Web/CSS/easing-function)
 */
export function createEasingFunction(ordinates: number[]): (t: number) => number {
  return t => {
    if (t <= 0) {
      return 0;
    }
    if (t >= 1) {
      return 1;
    }

    const x = t * (ordinates.length - 1);

    const startX = x | 0;
    const startY = ordinates[startX];

    return startY + (ordinates[startX + 1] - startY) * (x - startX);
  };
}
