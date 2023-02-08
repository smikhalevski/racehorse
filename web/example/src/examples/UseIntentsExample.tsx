import React from 'react';
import { useIntents } from '@racehorse/react';

export function UseIntentsExample() {
  const { openInExternalApplication } = useIntents();

  return (
    <>
      <h2>{'UseIntentsExample'}</h2>

      <button onClick={() => openInExternalApplication('https://github.com/smikhalevski/racehorse')}>
        {'Open in browser'}
      </button>
    </>
  );
}
