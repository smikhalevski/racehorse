import React from 'react';
import { activityManager, Intent } from 'racehorse';

export function ActivityExample() {
  return (
    <>
      <h2>{'Activity'}</h2>
      <button
        onClick={() => {
          activityManager.getActivityInfo().then(activityInfo => {
            activityManager.startActivity({
              // https://developer.android.com/reference/android/provider/Settings#ACTION_APP_NOTIFICATION_SETTINGS
              action: 'android.settings.APP_NOTIFICATION_SETTINGS',
              flags: Intent.FLAG_ACTIVITY_NEW_TASK,
              extras: {
                // https://developer.android.com/reference/android/provider/Settings#EXTRA_APP_PACKAGE
                'android.provider.extra.APP_PACKAGE': activityInfo.packageName,
              },
            });
          });
        }}
      >
        {'Open notification settings'}
      </button>
    </>
  );
}
