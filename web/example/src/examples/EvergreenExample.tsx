import React, { useMemo } from 'react';
import { eventBridge, evergreenManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';

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
        <p>
          <em>{'Select "release" build variant to see the bundle info.'}</em>
        </p>
      ) : (
        <FormattedJSON value={bundleInfo} />
      )}
    </>
  );
}
