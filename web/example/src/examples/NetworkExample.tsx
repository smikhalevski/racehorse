import React from 'react';
import { useNetworkStatus } from '@racehorse/react';
import { FormattedJSON } from '../components/FormattedJSON';

export function NetworkExample() {
  const networkStatus = useNetworkStatus();

  return (
    <>
      <h2>{'Network'}</h2>

      <p>
        {'Online '}
        <i
          className={networkStatus.isConnected ? 'bi-check-circle-fill text-success' : 'bi-x-circle-fill text-danger'}
        />
      </p>

      <FormattedJSON value={networkStatus} />
    </>
  );
}
