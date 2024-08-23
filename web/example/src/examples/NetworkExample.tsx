import React from 'react';
import { useNetworkStatus } from '@racehorse/react';
import { NetworkType } from 'racehorse';

export function NetworkExample() {
  const networkStatus = useNetworkStatus();

  return (
    <>
      <h1>{'Network'}</h1>

      <div className="mb-2">
        {networkStatus.isConnected ? (
          <>
            <i className="bi-check-circle-fill text-success me-2" />
            {'Connected'}
          </>
        ) : (
          <>
            <i className="bi-x-circle-fill text-danger me-2" />
            {'Disconnected'}
          </>
        )}
      </div>

      {
        {
          [NetworkType.WIFI]: (
            <>
              <i className="bi-wifi me-2" />
              {'Wifi'}
            </>
          ),
          [NetworkType.CELLULAR]: (
            <>
              <i className="bi-telephone me-2" />
              {'Cellular'}
            </>
          ),
          [NetworkType.NONE]: null,
          [NetworkType.UNKNOWN]: (
            <>
              <i className="bi-question-circle-fill text-secondary me-2" />
              {'Unknown network type'}
            </>
          ),
        }[networkStatus.type]
      }
    </>
  );
}
