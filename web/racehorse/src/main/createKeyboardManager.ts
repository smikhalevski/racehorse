import { EventBridge } from './createEventBridge';

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
      eventBridge.request({ type: 'org.racehorse.IsKeyboardVisibleEvent' }).payload.isKeyboardVisible,

    subscribe: listener =>
      eventBridge.subscribe('org.racehorse.KeyboardVisibilityChangedEvent', payload => {
        listener(payload.isKeyboardVisible);
      }),
  };
}
