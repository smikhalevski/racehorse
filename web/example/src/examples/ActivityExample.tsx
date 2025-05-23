import React, { useEffect, useMemo, useState } from 'react';
import { activityManager, ActivityResult, ActivityState, Intent, permissionsManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON.js';
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

  // Log activity state in console
  useEffect(
    () =>
      activityManager.subscribe(activityState => {
        console.log(activityStateLabel[activityState]);
      }),
    []
  );

  // Run action once when the app is in the background, or abort if the component unmounts
  useEffect(() => {
    const promise = activityManager.runIn(ActivityState.BACKGROUND, () => {
      console.log('Running in background');
    });

    return () => {
      promise.abort();
    };
  }, []);

  return (
    <>
      <h2>{'Activity'}</h2>

      <p>{'Activity state: ' + activityStateLabel[activityState]}</p>

      <p>
        <i>{'Open WebView console to observe activity state changes'}</i>
      </p>

      {'Activity info: '}
      <FormattedJSON value={activityInfo} />

      <p>
        <button
          onClick={() => {
            activityManager.startActivity({
              // https://developer.android.com/reference/android/provider/Settings#ACTION_APP_NOTIFICATION_SETTINGS
              action: 'android.settings.APP_NOTIFICATION_SETTINGS',
              flags: Intent.FLAG_ACTIVITY_NEW_TASK,
              extras: {
                // https://developer.android.com/reference/android/provider/Settings#EXTRA_APP_PACKAGE
                'android.provider.extra.APP_PACKAGE': activityManager.getActivityInfo().applicationId,
              },
            });
          }}
        >
          {'Open notification settings'}
        </button>
      </p>

      <p>
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
      </p>

      <button
        onClick={() => {
          activityManager.startActivity({
            action: 'android.intent.action.CHOOSER',
            extras: {
              'android.intent.extra.TITLE': 'Hello user!',
              'android.intent.extra.INTENT': {
                __javaClass: 'android.content.Intent',
                action: 'android.intent.action.SEND',
                type: 'text/plain',
                extras: {
                  'android.intent.extra.TEXT': 'This is a shared text',
                },
              },
            },
          });
        }}
      >
        {'Share text with chooser'}
      </button>

      <FormattedJSON value={contactActivityResult} />
    </>
  );
}
