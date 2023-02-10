import { Plugin } from './shared-types';

/**
 * Composes a plugin from multiple plugins.
 */
export function applyPlugins<A extends object, B extends object>(a: Plugin<A>, b: Plugin<B>): Plugin<A & B>;

/**
 * Composes a plugin from multiple plugins.
 */
export function applyPlugins<A extends object, B extends object, C extends object>(
  a: Plugin<A>,
  b: Plugin<B>,
  c: Plugin<C>
): Plugin<A & B & C>;

/**
 * Composes a plugin from multiple plugins.
 */
export function applyPlugins<A extends object, B extends object, C extends object, D extends object>(
  a: Plugin<A>,
  b: Plugin<B>,
  c: Plugin<C>,
  d: Plugin<D>
): Plugin<A & B & C & D>;

/**
 * Composes a plugin from multiple plugins.
 */
export function applyPlugins<A extends object, B extends object, C extends object, D extends object, E extends object>(
  a: Plugin<A>,
  b: Plugin<B>,
  c: Plugin<C>,
  d: Plugin<D>,
  e: Plugin<E>
): Plugin<A & B & C & D & E>;

/**
 * Composes a plugin from multiple plugins.
 */
export function applyPlugins<
  A extends object,
  B extends object,
  C extends object,
  D extends object,
  E extends object,
  F extends object
>(
  a: Plugin<A>,
  b: Plugin<B>,
  c: Plugin<C>,
  d: Plugin<D>,
  e: Plugin<E>,
  f: Plugin<F>,
  ...other: Plugin<object>[]
): Plugin<A & B & C & D & E & F>;

export function applyPlugins(...plugins: Plugin<object>[]): Plugin<object> {
  return (eventBridge, listener) => {
    let unsubscribeArray: Array<() => void> | undefined;

    for (const plugin of plugins) {
      const unsubscribe = plugin(eventBridge, listener);

      if (unsubscribe) {
        (unsubscribeArray ||= []).push(unsubscribe);
      }
    }
    if (!unsubscribeArray) {
      return;
    }
    return () => {
      for (const unsubscribe of unsubscribeArray!) {
        unsubscribe();
      }
    };
  };
}
