import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface KeyboardManager {
  isKeyboardVisible(): Promise<boolean>;

  /**
   * Subscribes a listener to software keyboard visibility changes.
   */
  subscribe(listener: (keyboardVisible: boolean) => void): () => void;
}

export function createKeyboardManager(eventBridge: EventBridge): KeyboardManager {
  return {
    isKeyboardVisible: () =>
      eventBridge
        .request({ type: 'org.racehorse.onIsKeyboardVisible' })
        .then(event => ensureEvent(event).isKeyboardVisible),

    subscribe: listener =>
      eventBridge.subscribe(event => {
        if (event.type === 'org.racehorse.KeyboardVisibilityChangedEvent') {
          listener(event.isKeyboardVisible);
        }
      }),
  };
}