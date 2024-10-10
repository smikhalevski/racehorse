import React from 'react';
import { facebookShareManager } from 'racehorse';

export function FacebookShareExample() {
  return (
    <>
      <h1>{'Facebook Share'}</h1>
      <button
        className="btn btn-primary"
        onClick={() => {
          facebookShareManager.shareLink({ contentUrl: window.location.href });
        }}
      >
        {'Share'}
      </button>
    </>
  );
}
