import React from 'react';
import { useNetworkStatus } from '@racehorse/react';
import { NetworkType } from 'racehorse';
import { Section } from '../components/Section';

export function NetworkExample() {
  const networkStatus = useNetworkStatus();

  return (
    <Section title={'Network'}>
      {networkStatus.isConnected ? (
        <>
          <i className="bi-check-circle-fill text-success me-2" />
          {
            {
              [NetworkType.WIFI]: 'Connected via Wi-Fi',
              [NetworkType.CELLULAR]: 'Connected via Cellular',
              [NetworkType.NONE]: null,
              [NetworkType.UNKNOWN]: 'Connected via unknown network type',
            }[networkStatus.type]
          }
        </>
      ) : (
        <>
          <i className="bi-x-circle-fill text-danger me-2" />
          {'Disconnected'}
        </>
      )}
    </Section>
  );
}
