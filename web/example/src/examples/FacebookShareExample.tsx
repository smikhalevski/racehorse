import React from 'react';
import { facebookShareManager } from 'racehorse';

export function FacebookShareExample() {
  return (
    <>
      <h2>{'Facebook Share'}</h2>
      <button
        onClick={() => {
          facebookShareManager.shareLink({ contentUrl: window.location.href });
        }}
      >
        {'Share'}
      </button>
    </>
  );
}
