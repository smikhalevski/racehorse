import React, { useEffect, useMemo, useState } from 'react';
import { activityManager, ActivityResult, ActivityState, Intent, permissionsManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';
import { useActivityState } from '@racehorse/react';

const activityStateLabel = {
  [ActivityState.BACKGROUND]: '🔴 Background',
  [ActivityState.FOREGROUND]: '🟡 Foreground',
  [ActivityState.ACTIVE]: '🟢 Active',
} as const;

export function ActivityExample() {
  const activityState = useActivityState();
  const activityInfo = useMemo(activityManager.getActivityInfo, []);
  const [contactActivityResult, setContactActivityResult] = useState<ActivityResult | null>();

  useEffect(
    () =>
      activityManager.subscribe(activityState => {
        console.log(activityStateLabel[activityState]);
      }),
    []
  );

  return (
    <>
      <h2>{'Activity'}</h2>

      <p>{'Activity state: ' + activityStateLabel[activityState]}</p>

      <p>
        <i>{'Open WebView console to observe activity state changes'}</i>
      </p>

      <p>
        {'Activity info: '}
        <FormattedJSON value={activityInfo} />
      </p>

      <p>
        <button
          onClick={() => {
            activityManager.startActivity({
              // https://developer.android.com/reference/android/provider/Settings#ACTION_APP_NOTIFICATION_SETTINGS
              action: 'android.settings.APP_NOTIFICATION_SETTINGS',
              flags: Intent.FLAG_ACTIVITY_NEW_TASK,
              extras: {
                // https://developer.android.com/reference/android/provider/Settings#EXTRA_APP_PACKAGE
                'android.provider.extra.APP_PACKAGE': activityManager.getActivityInfo().packageName,
              },
            });
          }}
        >
          {'Open notification settings'}
        </button>
      </p>

      <button
        onClick={() => {
          permissionsManager.askForPermission('android.permission.READ_CONTACTS').then(isGranted => {
            if (isGranted) {
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

      <FormattedJSON value={contactActivityResult} />
    </>
  );
}
