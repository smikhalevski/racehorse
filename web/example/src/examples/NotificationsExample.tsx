import React, { useEffect, useState } from 'react';
import { notificationsManager } from 'racehorse';

export function NotificationsExample() {
  const [areNotificationsEnabled, setAreNotificationsEnabled] = useState(notificationsManager.areNotificationsEnabled);

  useEffect(() => {
    const visibilityListener = () => {
      setAreNotificationsEnabled(notificationsManager.areNotificationsEnabled());
    };

    window.addEventListener('visibilitychange', visibilityListener);

    return () => {
      window.removeEventListener('visibilitychange', visibilityListener);
    };
  }, []);

  return (
    <>
      <h1>{'Notifications'}</h1>

      <p>
        {'Notifications enabled: '}
        {areNotificationsEnabled ? '🟢' : '🔴'}
      </p>
    </>
  );
}
