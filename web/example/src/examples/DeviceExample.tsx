import React, { useEffect, useState } from 'react';
import { DeviceInfo, deviceManager, Rect } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';

export function DeviceExample() {
  const [deviceInfo, setDeviceInfo] = useState<DeviceInfo>();
  const [preferredLocales, setPreferredLocales] = useState<string[]>([]);
  const [insets, setInsets] = useState<Rect>();

  useEffect(() => {
    deviceManager.getDeviceInfo().then(setDeviceInfo);
    deviceManager.getPreferredLocales().then(setPreferredLocales);
    deviceManager.getWindowInsets().then(setInsets);
  }, []);

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
