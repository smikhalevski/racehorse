import React from 'react';
import { useNetworkStatus } from '@racehorse/react';
import { FormattedJSON } from '../components/FormattedJSON.js';

export function NetworkExample() {
  const networkStatus = useNetworkStatus();

  return (
    <>
      <h2>{'Network'}</h2>

      <p>
        {'Online: '}
        {networkStatus.isConnected ? '🟢' : '🔴'}
      </p>

      <FormattedJSON value={networkStatus} />
    </>
  );
}
