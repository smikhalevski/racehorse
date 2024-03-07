/**
 * @template T The result of an action.
 */
export interface Joiner<T> {
  /**
   * Returns `true` if joiner currently has a pending action, or `false` otherwise.
   */
  isPending(): boolean;

  /**
   * Invokes the action. If there's a pending action than this call would return its promise, and the given
   * action callback won't be called.
   *
   * @param action The action to invoke.
   */
  join(action: () => Promise<T>): Promise<T>;
}

/**
 * Joiner invokes the action callback if there's no pending callback, otherwise it returns the result of the pending
 * action.
 *
 * @template T The result of an action.
 */
export function createJoiner<T>(): Joiner<T> {
  let promise: Promise<T> | undefined;

  return {
    isPending: () => promise !== undefined,

    join: action =>
      (promise ||= action().then(value => {
        promise = undefined;
        return value;
      })),
  };
}
