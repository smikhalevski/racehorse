import React from 'react';
import { actionsPlugin } from 'racehorse';
import { useEventBridge } from '@racehorse/react';

export function UseIntentsExample() {
  const { openUrl } = useEventBridge(actionsPlugin);

  return (
    <>
      <h2>{'UseIntentsExample'}</h2>

      <button onClick={() => openUrl('https://github.com/smikhalevski/racehorse')}>{'Open in browser'}</button>
    </>
  );
}
