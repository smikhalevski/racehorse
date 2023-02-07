import { useConfiguration, useIntents, useOnline, usePermissions } from '@racehorse/react';
import { useEffect, useState } from 'react';

module.hot?.accept(() => {
  location.reload();
});

export function App() {
  const [locales, setLocales] = useState<string[]>();
  const [permissions, setPermissions] = useState<any>();
  const [geolocation, setGeolocation] = useState<any>();

  const online = useOnline();
  const { openInExternalApplication } = useIntents();
  const { getPreferredLocales } = useConfiguration();
  const { askForPermission } = usePermissions();

  useEffect(() => {
    getPreferredLocales().then(setLocales);
  }, []);

  return (
    <>
      <h1>
        {'Online: '}
        {online === undefined ? '🟡' : online ? '🟢' : '🔴'}
      </h1>

      <hr />

      <h1>
        {'Locales: '}
        {locales?.join(',')}
      </h1>

      <hr />

      <button onClick={() => openInExternalApplication('https://github.com/smikhalevski')}>
        {'Open in external browser2'}
      </button>

      <hr />

      <input
        type="file"
        accept="image/*"
        multiple={true}
        onChange={event => {
          console.log(event);
        }}
      />

      <hr />

      <button
        onClick={() => {
          navigator.geolocation.getCurrentPosition(
            position => {
              setGeolocation({ latitude: position.coords.latitude, longitude: position.coords.longitude });
            },
            positionError => {
              setGeolocation(positionError.message);
            }
          );
        }}
      >
        {'Get geolocation'}
      </button>

      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(geolocation, null, 2)}</pre>

      <hr />

      <button
        onClick={() => {
          askForPermission([
            // 'android.permission.ACCESS_WIFI_STATE',
            // 'android.permission.ACCESS_NETWORK_STATE',
            // 'android.permission.CHANGE_WIFI_STATE',
            // 'android.permission.CALL_PHONE',
            // 'android.permission.READ_EXTERNAL_STORAGE',
            'android.permission.CAMERA',
            // 'android.permission.ACCESS_FINE_LOCATION',
            // 'android.permission.ACCESS_COARSE_LOCATION',
          ]).then(setPermissions);
        }}
      >
        {'Request permission'}
      </button>

      <h1>{'Result'}</h1>

      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(permissions, null, 2)}</pre>
    </>
  );
}
