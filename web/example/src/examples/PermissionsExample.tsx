import React, { useState } from 'react';
import { permissionsManager } from 'racehorse';

export function PermissionsExample() {
  const [permissions, setPermissions] = useState<any>();

  return (
    <>
      <h2>{'Permissions'}</h2>

      <button
        onClick={() => {
          permissionsManager
            .askForPermission([
              // 'android.permission.ACCESS_WIFI_STATE',
              // 'android.permission.ACCESS_NETWORK_STATE',
              // 'android.permission.CHANGE_WIFI_STATE',
              // 'android.permission.CALL_PHONE',
              // 'android.permission.READ_EXTERNAL_STORAGE',
              'android.permission.CAMERA',
              // 'android.permission.ACCESS_FINE_LOCATION',
              // 'android.permission.ACCESS_COARSE_LOCATION',
            ])
            .then(setPermissions);
        }}
      >
        {'Request permission'}
      </button>

      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(permissions, null, 2)}</pre>
    </>
  );
}
