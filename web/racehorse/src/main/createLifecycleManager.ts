import { EventBridge } from './createEventBridge';
import { noop } from './utils';

export const LifecycleState = {
  STARTED: 0,
  RESUMED: 1,
  PAUSED: 2,
  STOPPED: 3,
} as const;

export type LifecycleState = (typeof LifecycleState)[keyof typeof LifecycleState];

export interface LifecycleManager {
  /**
   * Returns the lifecycle state.
   */
  getLifecycleState(): LifecycleState;

  /**
   * Subscribes a listener to lifecycle state changes.
   */
  subscribe(listener: (state: LifecycleState) => void): () => void;

  subscribe(state: 'start', listener: () => void): () => void;

  subscribe(state: 'resume', listener: () => void): () => void;

  subscribe(state: 'pause', listener: () => void): () => void;

  subscribe(state: 'stop', listener: () => void): () => void;
}

const ordinals = {
  start: 0,
  resume: 1,
  pause: 2,
  stop: 3,
};

export function createLifecycleManager(eventBridge: EventBridge): LifecycleManager {
  return {
    getLifecycleState: () => eventBridge.request({ type: 'org.racehorse.GetLifecycleStateEvent' }).payload.state,

    subscribe: (state, listener = state) => {
      const ordinal = typeof state === 'string' ? ordinals[state] : -1;

      if (ordinal === undefined || typeof listener !== 'function') {
        return noop;
      }

      return eventBridge.subscribe(
        'org.racehorse.LifecycleStateChangeEvent',
        ordinal === -1
          ? payload => listener(payload.state)
          : payload => {
              if (ordinal === payload.state) {
                listener();
              }
            }
      );
    },
  };
}
