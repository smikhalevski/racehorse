import { EventBridge } from './createEventBridge.js';
import { TweenAnimation, Unsubscribe } from './types.js';
import { createEasing } from './easing.js';

export interface KeyboardManager {
  /**
   * Returns the current height of the software keyboard.
   *
   * If 0 then the keyboard is hidden, if greater than 0 then the keyboard is shown.
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
   * Subscribes a listener to the keyboard show and hide animation start.
   */
  subscribe(listener: (animation: TweenAnimation) => void): Unsubscribe;
}

/**
 * Toggles the software keyboard and notifies about keyboard animation.
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

    subscribe: listener =>
      eventBridge.subscribe('org.racehorse.KeyboardAnimationStartedEvent', payload => {
        const animation = Object.assign({}, payload.animation);

        animation.easing = createEasing(payload.animation.easing);

        listener(animation);
      }),
  };
}
