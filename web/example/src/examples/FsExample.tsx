import React from 'react';
import { fs, SystemDir } from 'racehorse';

export function FsExample() {
  return (
    <>
      <h2>{'Filesystem'}</h2>

      <button
        onClick={async () => {
          const file = fs.File(SystemDir.CACHE, 'test.txt');

          await file.writeText('Hello world');

          console.log(file.uri, await file.readText());
        }}
      >
        {'Create test file'}
      </button>
    </>
  );
}
