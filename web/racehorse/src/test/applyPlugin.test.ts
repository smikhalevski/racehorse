import { applyPlugins, createEventBridge } from '../main';

describe('applyPlugins', () => {
  // test('invokes all plugins', () => {
  //   const plugin1Mock = jest.fn();
  //   const plugin2Mock = jest.fn();
  //   const listener = () => undefined;
  //   const eventBridge = createEventBridge();
  //
  //   const plugin = applyPlugins(plugin1Mock, plugin2Mock);
  //
  //   expect(plugin).toBeInstanceOf(Function);
  //   expect(plugin(eventBridge, listener)).toBeInstanceOf(Function);
  //
  //   expect(plugin1Mock).toHaveBeenCalledTimes(1);
  //   expect(plugin1Mock).toHaveBeenNthCalledWith(1, eventBridge, listener);
  //
  //   expect(plugin2Mock).toHaveBeenCalledTimes(1);
  //   expect(plugin2Mock).toHaveBeenNthCalledWith(1, eventBridge, listener);
  // });
  //
  // test('invokes unsubscribe for all plugins', () => {
  //   const unsubscribe1Mock = jest.fn();
  //   const unsubscribe2Mock = jest.fn();
  //
  //   const plugin1 = () => unsubscribe1Mock;
  //   const plugin2 = () => unsubscribe2Mock;
  //
  //   const plugin = applyPlugins(plugin1, plugin2);
  //
  //   plugin(createEventBridge(), noop)?.();
  //
  //   expect(unsubscribe1Mock).toHaveBeenCalledTimes(1);
  //   expect(unsubscribe2Mock).toHaveBeenCalledTimes(1);
  // });
  //
  // test('plugin can return undefined instead of an unsubscribe callback', () => {
  //   const unsubscribe1Mock = jest.fn();
  //
  //   const plugin1 = () => unsubscribe1Mock;
  //   const plugin2 = () => undefined;
  //
  //   const plugin = applyPlugins(plugin1, plugin2);
  //
  //   plugin(createEventBridge(), noop)?.();
  //
  //   expect(unsubscribe1Mock).toHaveBeenCalledTimes(1);
  // });
});
