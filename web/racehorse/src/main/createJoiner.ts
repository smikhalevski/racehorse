/**
 * @template T The operation result.
 */
export interface Joiner<T> {
  /**
   * Returns `true` if joiner currently has a pending operation, or `false` otherwise.
   */
  isPending(): boolean;

  /**
   * Invokes the operation. If there's a pending operation than this call would return its promise, and the given
   * operation callback won't be called.
   *
   * @param operation The operation to invoke.
   */
  join(operation: () => Promise<T>): Promise<T>;
}

/**
 * Joiner invokes the operation callback if there's no pending callback, otherwise it returns the result of the pending
 * operation.
 *
 * @template T The operation result.
 */
export function createJoiner<T>(): Joiner<T> {
  let promise: Promise<T> | undefined;

  return {
    isPending: () => promise !== undefined,

    join: operation =>
      (promise ||= operation().then(result => {
        promise = undefined;
        return result;
      })),
  };
}
