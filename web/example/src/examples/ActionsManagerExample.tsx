import React from 'react';
import { actionsManager } from 'racehorse';

export function ActionsManagerExample() {
  return (
    <>
      <h2>{'Actions'}</h2>

      <button onClick={() => actionsManager.openUrl('https://github.com/smikhalevski/racehorse')}>
        {'Open in browser'}
      </button>
    </>
  );
}
