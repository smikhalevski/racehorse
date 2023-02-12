import React from 'react';
import { devicePlugin } from 'racehorse';
import { useEventBridge } from '@racehorse/react';

export function UseConfigurationExample() {
  const { getPreferredLocales } = useEventBridge(devicePlugin);

  return (
    <>
      <h2>{'UseConfigurationExample'}</h2>

      {'Locales: '}
      {getPreferredLocales().join(',')}
    </>
  );
}
