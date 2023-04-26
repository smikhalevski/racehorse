import React, { useEffect, useState } from 'react';
import { DeviceInfo, deviceManager, Rect } from 'racehorse';

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
      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(insets, null, 2)}</pre>

      {'Device info: '}
      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(deviceInfo, null, 2)}</pre>
    </>
  );
}
