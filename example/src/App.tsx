import { Event } from 'racehorse';
import { useEventBridge, useEventBridgeSubscription } from '@racehorse/react';
import { Fragment, useState } from 'react';

export function App() {
  const [events, setEvents] = useState<Event[]>([]);

  const eventBridge = useEventBridge();

  useEventBridgeSubscription(event => {
    setEvents(events => events.concat(event));
  });

  return (
    <>
      <button
        onClick={() => {
          eventBridge
            .request({
              type: 'org.racehorse.permissions.IsPermissionGrantedRequestEvent',
              permissions: [
                'android.permission.ACCESS_WIFI_STATE',
                'android.permission.ACCESS_NETWORK_STATE',
                'android.permission.CHANGE_WIFI_STATE',
                'android.permission.CALL_PHONE',
                'android.permission.READ_EXTERNAL_STORAGE',
                'android.permission.CAMERA',
                'android.permission.ACCESS_FINE_LOCATION',
                'android.permission.ACCESS_COARSE_LOCATION',
              ],
            })
            .then(event => {
              setEvents(events => events.concat(event));
            });
        }}
      >
        {'Request permission'}
      </button>

      <h1>{'Events'}</h1>

      {events.map((event, i) => (
        <Fragment key={i}>
          <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(event, null, 2)}</pre>
          <hr />
        </Fragment>
      ))}
    </>
  );
}
