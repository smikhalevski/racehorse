import React, { useEffect, useState } from 'react';
import { configurationManager } from 'racehorse';

export function ConfigurationManagerExample() {
  const [preferredLocales, setPreferredLocales] = useState<string[]>([]);

  useEffect(() => {
    configurationManager.getPreferredLocales().then(setPreferredLocales);
  }, []);

  return (
    <>
      <h2>{'ConfigurationManagerExample'}</h2>

      {'Locales: '}
      {preferredLocales.join(',')}
    </>
  );
}
