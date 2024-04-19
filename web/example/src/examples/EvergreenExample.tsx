import React, { useMemo } from 'react';
import { eventBridge, evergreenManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';

export function EvergreenExample() {
  const bundleInfo = useMemo(() => {
    if (eventBridge.isSupported('org.racehorse.evergreen.GetBundleInfoEvent')) {
      return evergreenManager.getBundleInfo();
    }
  }, []);

  if (bundleInfo === undefined) {
    return null;
  }

  return (
    <>
      <h2>{'Evergreen'}</h2>

      <FormattedJSON value={bundleInfo} />
    </>
  );
}
