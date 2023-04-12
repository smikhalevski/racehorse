import React, { useEffect, useState } from 'react';
import { deviceManager, Rect } from 'racehorse';

export function DeviceExample() {
  const [preferredLocales, setPreferredLocales] = useState<string[]>([]);
  const [insets, setInsets] = useState<Rect>();

  useEffect(() => {
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
    </>
  );
}
