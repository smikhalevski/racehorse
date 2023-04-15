import React from 'react';
import { useNetworkStatus } from '@racehorse/react';

export function NetworkExample() {
  const networkStatus = useNetworkStatus();

  return (
    <>
      <h2>{'Network'}</h2>

      {'Online: '}
      {networkStatus.isConnected === undefined ? '🟡' : networkStatus.isConnected ? '🟢' : '🔴'}
    </>
  );
}