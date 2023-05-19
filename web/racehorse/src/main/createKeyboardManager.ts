import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface KeyboardManager {
  isKeyboardVisible(): boolean;

  /**
   * Subscribes a listener to software keyboard visibility changes.
   */
  subscribe(listener: (keyboardVisible: boolean) => void): () => void;
}

export function createKeyboardManager(eventBridge: EventBridge): KeyboardManager {
  return {
    isKeyboardVisible: () =>
      ensureEvent(eventBridge.requestSync({ type: 'org.racehorse.IsKeyboardVisibleEvent' })).payload.isKeyboardVisible,

    subscribe: listener =>
      eventBridge.subscribe(event => {
        if (event.type === 'org.racehorse.KeyboardVisibilityChangedEvent') {
          listener(event.payload.isKeyboardVisible);
        }
      }),
  };
}
