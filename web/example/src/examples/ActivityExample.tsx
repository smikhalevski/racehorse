import React, { useState } from 'react';
import { activityManager, ActivityResult, Intent, permissionsManager } from 'racehorse';

export function ActivityExample() {
  const [contactActivityResult, setContactActivityResult] = useState<ActivityResult | null>();
  return (
    <>
      <h2>{'Activity'}</h2>

      <p>
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
      </p>

      <button
        onClick={() => {
          permissionsManager.askForPermission('android.permission.READ_CONTACTS').then(granted => {
            if (granted) {
              activityManager
                .startActivityForResult({
                  // https://developer.android.com/guide/components/intents-common#Contacts
                  action: 'android.intent.action.PICK',
                  type: 'vnd.android.cursor.dir/contact',
                })
                .then(setContactActivityResult);
            }
          });
        }}
      >
        {'Pick a contact'}
      </button>

      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(contactActivityResult, null, 2)}</pre>
    </>
  );
}
