import React, { useState } from 'react';
import { usePermissions } from '@racehorse/react';

export function UsePermissionsExample() {
  const [permissions, setPermissions] = useState<any>();

  const { askForPermission } = usePermissions();

  return (
    <>
      <h2>{'UsePermissionsExample'}</h2>

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

      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(permissions, null, 2)}</pre>
    </>
  );
}
