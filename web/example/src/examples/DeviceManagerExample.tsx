import React, { useState } from 'react';
import { deviceManager } from 'racehorse';

export function DeviceManagerExample() {
  const [preferredLocales] = useState(deviceManager.getPreferredLocales);

  return (
    <>
      <h2>{'DeviceManagerExample'}</h2>

      {'Locales: '}
      {preferredLocales.join(',')}
    </>
  );
}
