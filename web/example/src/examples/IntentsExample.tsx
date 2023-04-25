import React from 'react';
import { intentsManager } from 'racehorse';

export function IntentsExample() {
  return (
    <>
      <h2>{'Intents'}</h2>
      <button
        onClick={() => {
          intentsManager.openApplication('https://github.com/smikhalevski/racehorse');
        }}
      >
        {'Open in browser'}
      </button>{' '}
      <button
        onClick={() => {
          intentsManager.startActivity({
            action: 'android.intent.action.VIEW',
            uri: 'https://github.com/smikhalevski/racehorse',
            extras: { foo: [123] },
          });
        }}
      >
        {'View in browser'}
      </button>
    </>
  );
}
