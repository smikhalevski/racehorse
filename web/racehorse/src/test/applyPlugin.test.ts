import { applyPlugins, createEventBridge } from '../main';

describe('applyPlugins', () => {
  test('invokes all plugins', () => {
    const plugin1Mock = jest.fn();
    const plugin2Mock = jest.fn();
    const listener = () => undefined;
    const eventBridge = createEventBridge();

    const plugin = applyPlugins(plugin1Mock, plugin2Mock);

    expect(plugin).toBeInstanceOf(Function);

    plugin(eventBridge, listener);

    expect(plugin1Mock).toHaveBeenCalledTimes(1);
    expect(plugin1Mock).toHaveBeenNthCalledWith(1, eventBridge, listener);

    expect(plugin2Mock).toHaveBeenCalledTimes(1);
    expect(plugin2Mock).toHaveBeenNthCalledWith(1, eventBridge, listener);
  });
});
