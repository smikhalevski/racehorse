import React, { useEffect, useState } from 'react';
import { configurationManager, Rect } from 'racehorse';

export function ConfigurationManagerExample() {
  const [preferredLocales, setPreferredLocales] = useState<string[]>([]);
  const [insets, setInsets] = useState<Rect>();

  useEffect(() => {
    configurationManager.getPreferredLocales().then(setPreferredLocales);
    configurationManager.getWindowInsets().then(setInsets);
  }, []);

  return (
    <>
      <h2>{'Configuration'}</h2>

      <p>
        {'Locales: '}
        {preferredLocales.join(',')}
      </p>

      {'Insets: '}
      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(insets, null, 2)}</pre>
    </>
  );
}
