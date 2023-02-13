import { useEventBridge } from '../main';
import { actionsPlugin, applyPlugins, networkPlugin, permissionsPlugin } from 'racehorse';

describe('useEventBridge', () => {
  test('', () => {
    const eventBridge = useEventBridge(applyPlugins(networkPlugin, actionsPlugin, permissionsPlugin));

    eventBridge.online;
  });
});
