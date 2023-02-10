import { Plugin } from './shared-types';
import { noop } from './utils';

/**
 * Composes a plugin from multiple plugins.
 */
export function applyPlugins<A, B>(a: Plugin<A>, b: Plugin<B>): Plugin<A & B>;

/**
 * Composes a plugin from multiple plugins.
 */
export function applyPlugins<A, B, C>(a: Plugin<A>, b: Plugin<B>, c: Plugin<C>): Plugin<A & B & C>;

/**
 * Composes a plugin from multiple plugins.
 */
export function applyPlugins<A, B, C, D>(a: Plugin<A>, b: Plugin<B>, c: Plugin<C>, d: Plugin<D>): Plugin<A & B & C & D>;

/**
 * Composes a plugin from multiple plugins.
 */
export function applyPlugins<A, B, C, D, E>(
  a: Plugin<A>,
  b: Plugin<B>,
  c: Plugin<C>,
  d: Plugin<D>,
  e: Plugin<E>
): Plugin<A & B & C & D & E>;

/**
 * Composes a plugin from multiple plugins.
 */
export function applyPlugins<A, B, C, D, E, F>(
  a: Plugin<A>,
  b: Plugin<B>,
  c: Plugin<C>,
  d: Plugin<D>,
  e: Plugin<E>,
  f: Plugin<F>,
  ...other: Plugin<unknown>[]
): Plugin<A & B & C & D & E & F>;

export function applyPlugins(...plugins: Plugin<unknown>[]): Plugin<unknown> {
  return (eventBridge, listener) => {
    let unsubscribeArray: Array<() => void> | undefined;

    for (const plugin of plugins) {
      const unsubscribe = plugin(eventBridge, listener);

      if (unsubscribe) {
        (unsubscribeArray ||= []).push(unsubscribe);
      }
    }

    if (!unsubscribeArray) {
      return noop;
    }

    return () => {
      for (const unsubscribe of unsubscribeArray!) {
        unsubscribe();
      }
    };
  };
}
