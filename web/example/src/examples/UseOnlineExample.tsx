import React from 'react';
import { networkPlugin } from 'racehorse';
import { useEventBridge } from '@racehorse/react';

export function UseOnlineExample() {
  const { online } = useEventBridge(networkPlugin);

  return (
    <>
      <h2>{'UseOnlineExample'}</h2>

      {'Online: '}
      {online === undefined ? '🟡' : online ? '🟢' : '🔴'}
    </>
  );
}
