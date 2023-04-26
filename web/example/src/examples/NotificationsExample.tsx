import React, { useEffect, useState } from 'react';
import { notificationsManager } from 'racehorse';

export function NotificationsExample() {
  const [areNotificationsEnabled, setAreNotificationsEnabled] = useState<boolean>();

  useEffect(() => {
    const listener = () => notificationsManager.areNotificationsEnabled().then(setAreNotificationsEnabled);

    listener();

    window.addEventListener('visibilitychange', listener);

    return () => {
      window.removeEventListener('visibilitychange', listener);
    };
  }, []);

  return (
    <>
      <h2>{'Notifications'}</h2>

      <p>
        {'Notifications enabled: '}
        {areNotificationsEnabled === undefined ? '🟡' : areNotificationsEnabled ? '🟢' : '🔴'}
      </p>
    </>
  );
}
