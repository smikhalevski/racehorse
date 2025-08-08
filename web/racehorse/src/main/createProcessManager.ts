import { EventBridge } from './createEventBridge.js';
import { noop } from './utils.js';
import { Unsubscribe } from './types.js';

export const ProcessState = {
  /**
   * A process is in the background and not visible to the user.
   */
  BACKGROUND: 0,

  /**
   * A process is in the foreground, and it is visible to the user, but doesn't have focus (for example, covered by a
   * dialog).
   */
  FOREGROUND: 1,

  /**
   * A process is in the foreground and user can interact with it.
   */
  ACTIVE: 2,
} as const;

export type ProcessState = (typeof ProcessState)[keyof typeof ProcessState];

export interface ProcessManager {
  /**
   * Get the state of the process.
   */
  getProcessState(): ProcessState;

  /**
   * Subscribes a listener to process status changes.
   */
  subscribe(listener: (processState: ProcessState) => void): Unsubscribe;

  /**
   * The process went to background: user doesn't see the process anymore.
   */
  subscribe(eventType: 'background', listener: () => void): Unsubscribe;

  /**
   * The process entered foreground: user can see the process but cannot interact with it.
   */
  subscribe(eventType: 'foreground', listener: () => void): Unsubscribe;

  /**
   * The process became active: user can see the process and can interact with it.
   */
  subscribe(eventType: 'active', listener: () => void): Unsubscribe;
}

const eventTypeToProcessState = {
  background: ProcessState.BACKGROUND,
  foreground: ProcessState.FOREGROUND,
  active: ProcessState.ACTIVE,
} as const;

/**
 * Monitors the application process.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createProcessManager(eventBridge: EventBridge): ProcessManager {
  return {
    getProcessState: () => eventBridge.request({ type: 'org.racehorse.GetProcessStateEvent' }).payload.state,

    subscribe: (eventTypeOrListener, listener = eventTypeOrListener) => {
      const expectedState = typeof eventTypeOrListener === 'string' ? eventTypeToProcessState[eventTypeOrListener] : -1;

      if (expectedState === undefined || typeof listener !== 'function') {
        return noop;
      }

      return eventBridge.subscribe(
        'org.racehorse.ProcessStateChangedEvent',
        expectedState === -1
          ? payload => listener(payload.state)
          : payload => {
              if (expectedState === payload.state) {
                listener();
              }
            }
      );
    },
  };
}
