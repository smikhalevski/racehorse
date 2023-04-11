import React from 'react';
import { useOnline } from '@racehorse/react';

export function UseOnlineExample() {
  const online = useOnline();

  return (
    <>
      <h2>{'useOnline hook'}</h2>

      {'Online: '}
      {online === undefined ? '🟡' : online ? '🟢' : '🔴'}
    </>
  );
}
