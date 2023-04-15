import React from 'react';
import { openUrlManager } from 'racehorse';

export function OpenUrlExample() {
  return (
    <>
      <h2>{'Open URL'}</h2>

      <button
        onClick={() => {
          openUrlManager.openUrl('https://github.com/smikhalevski/racehorse');
        }}
      >
        {'Open in browser'}
      </button>
    </>
  );
}
