import React, { useMemo } from 'react';
import { deviceManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';

export function DeviceExample() {
  const deviceInfo = useMemo(deviceManager.getDeviceInfo, []);
  const preferredLocales = useMemo(deviceManager.getPreferredLocales, []);
  const insets = useMemo(deviceManager.getWindowInsets, []);

  return (
    <>
      <h2>{'Device'}</h2>

      <p>
        {'Locales: '}
        {preferredLocales.join(',')}
      </p>

      {'Insets:'}
      <FormattedJSON value={insets} />

      {'Device info: '}
      <FormattedJSON value={deviceInfo} />
    </>
  );
}
