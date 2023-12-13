import React, { useEffect } from 'react';
import { lifecycleManager, LifecycleState } from 'racehorse';

const messages = {
  [LifecycleState.STARTED]: 'Started',
  [LifecycleState.RESUMED]: 'Resumed',
  [LifecycleState.PAUSED]: 'Paused',
  [LifecycleState.STOPPED]: 'Stopped',
};

export function LifecycleExample() {
  useEffect(() => {
    console.log(messages[lifecycleManager.getLifecycleState()]);

    return lifecycleManager.subscribe(state => {
      console.log(messages[state]);
    });
  }, []);

  return (
    <>
      <h2>{'Lifecycle'}</h2>

      <p>{'Open WebView console to observe lifecycle events'}</p>
    </>
  );
}
