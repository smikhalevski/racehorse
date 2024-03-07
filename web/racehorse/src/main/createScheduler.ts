import { AbortableCallback, AbortablePromise } from 'parallel-universe';

export interface Scheduler {
  /**
   * Returns `true` if scheduler currently has a pending action, or `false` otherwise.
   */
  isPending(): boolean;

  /**
   * Schedules an action callback invocation after all previously scheduled actions are completed.
   *
   * The action callback receives a non-aborted signal.
   *
   * @param action The action to schedule.
   */
  schedule<T>(action: AbortableCallback<T>): AbortablePromise<T>;
}

/**
 * Schedules an action callback invocation after all previously scheduled actions are completed.
 */
export function createScheduler(): Scheduler {
  let promise = Promise.resolve();
  let isPending = false;

  return {
    isPending: () => isPending,

    schedule: action => {
      isPending = true;

      return new AbortablePromise((resolve, reject, signal) => {
        const actionPromise = (promise = promise
          .then(() => {
            if (signal.aborted) {
              throw signal.reason;
            }
            return action(signal);
          })
          .then(
            value => {
              isPending = promise !== actionPromise;
              resolve(value);
            },
            reason => {
              isPending = promise !== actionPromise;
              reject(reason);
            }
          ));
      });
    },
  };
}
