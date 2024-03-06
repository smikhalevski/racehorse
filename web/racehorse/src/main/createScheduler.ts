import { noop } from './utils';

export interface Scheduler {
  /**
   * Returns `true` if scheduler currently has a pending operation, or `false` otherwise.
   */
  isPending(): boolean;

  /**
   * Schedules an operation callback invocation after all previously scheduled operations are completed.
   *
   * @param operation The operation to schedule.
   */
  schedule<T>(operation: () => PromiseLike<T> | T): Promise<T>;
}

/**
 * Schedules an operation callback invocation after all previously scheduled operations are completed.
 */
export function createScheduler(): Scheduler {
  let promise: Promise<unknown> = Promise.resolve();
  let isPending = false;

  return {
    isPending: () => isPending,

    schedule: operation => {
      isPending = true;

      const operationPromise = (promise = promise
        .then(noop, noop)
        .then(operation)
        .then(
          value => {
            isPending = promise !== operationPromise;
            return value;
          },
          reason => {
            isPending = promise !== operationPromise;
            throw reason;
          }
        ));

      return operationPromise;
    },
  };
}
