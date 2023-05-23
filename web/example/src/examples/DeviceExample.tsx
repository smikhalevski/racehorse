import React, { useState } from 'react';
import { deviceManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';

export function DeviceExample() {
  const [deviceInfo] = useState(deviceManager.getDeviceInfo);
  const [preferredLocales] = useState(deviceManager.getPreferredLocales);
  const [insets] = useState(deviceManager.getWindowInsets);

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
