import React, { useEffect, useState } from 'react';
import { useConfiguration } from '@racehorse/react';

module.hot?.accept(() => {
  location.reload();
});

export function UseConfigurationExample() {
  const [locales, setLocales] = useState<string[]>();
  const { getPreferredLocales } = useConfiguration();

  useEffect(() => {
    getPreferredLocales().then(setLocales);
  }, []);

  return (
    <>
      <h2>{'UseConfigurationExample'}</h2>

      {'Locales: '}
      {locales?.join(',')}
    </>
  );
}
