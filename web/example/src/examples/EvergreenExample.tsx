import React, { useMemo } from 'react';
import { eventBridge, evergreenManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON.js';

export function EvergreenExample() {
  const bundleInfo = useMemo(() => {
    if (eventBridge.isSupported('org.racehorse.evergreen.GetBundleInfoEvent')) {
      return evergreenManager.getBundleInfo();
    }
  }, []);

  return (
    <>
      <h2>{'Evergreen'}</h2>

      {bundleInfo === undefined ? (
        <em>{'Select "release" build variant to see the bundle info.'}</em>
      ) : (
        <FormattedJSON value={bundleInfo} />
      )}
    </>
  );
}
