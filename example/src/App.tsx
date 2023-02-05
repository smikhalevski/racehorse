import { useOnlineStatus, usePermissionManager } from '@racehorse/react';
import { useState } from 'react';

module.hot?.accept(() => {
  location.reload();
});

export function App() {
  const [value, setValue] = useState<any>();

  const online = useOnlineStatus();
  const { askForPermission } = usePermissionManager();

  return (
    <>
      <h1>
        {'Online: '}
        {online === undefined ? '🟡' : online ? '🟢' : '🔴'}
      </h1>

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
          ]).then(setValue);
        }}
      >
        {'Request permission'}
      </button>

      <h1>{'Result'}</h1>

      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(value, null, 2)}</pre>
    </>
  );
}
