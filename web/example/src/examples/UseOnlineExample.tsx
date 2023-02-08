import React from 'react';
import { useOnline } from '@racehorse/react';

module.hot?.accept(() => {
  location.reload();
});

export function UseOnlineExample() {
  const online = useOnline();

  return (
    <>
      <h2>{'UseOnlineExample'}</h2>

      {'Online: '}
      {online === undefined ? '🟡' : online ? '🟢' : '🔴'}
    </>
  );
}
