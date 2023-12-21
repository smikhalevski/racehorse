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
  schedule<T>(operation: () => Promise<T>): Promise<T>;
}

/**
 * Schedules an operation callback invocation after all previously scheduled operations are completed.
 */
export function createScheduler(): Scheduler {
  let promise: Promise<unknown> = Promise.resolve();
  let pending = false;

  return {
    isPending: () => pending,

    schedule: operation => {
      pending = true;

      const operationPromise = promise
        .then(noop, noop)
        .then(operation)
        .then(
          result => {
            pending = promise !== operationPromise;
            return result;
          },
          reason => {
            pending = promise !== operationPromise;
            throw reason;
          }
        );

      return (promise = operationPromise);
    },
  };
}
