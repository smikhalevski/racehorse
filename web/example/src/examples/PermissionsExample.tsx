import React, { useState } from 'react';
import { permissionsManager } from 'racehorse';

export function PermissionsExample() {
  const [permissions, setPermissions] = useState<string[]>([]);
  const [statuses, setStatuses] = useState<any>();

  return (
    <>
      <h2>{'Permissions'}</h2>

      <p>
        {'Permissions: '}
        <select
          multiple={true}
          value={permissions}
          onChange={event => {
            setPermissions(Array.from(event.target.selectedOptions).map(option => option.value));
          }}
        >
          <option value={'android.permission.ACCESS_WIFI_STATE'}>{'ACCESS_WIFI_STATE'}</option>
          <option value={'android.permission.ACCESS_NETWORK_STATE'}>{'ACCESS_NETWORK_STATE'}</option>
          <option value={'android.permission.CHANGE_WIFI_STATE'}>{'CHANGE_WIFI_STATE'}</option>
          <option value={'android.permission.CALL_PHONE'}>{'CALL_PHONE'}</option>
          <option value={'android.permission.READ_EXTERNAL_STORAGE'}>{'READ_EXTERNAL_STORAGE'}</option>
          <option value={'android.permission.CAMERA'}>{'CAMERA'}</option>
          <option value={'android.permission.ACCESS_FINE_LOCATION'}>{'ACCESS_FINE_LOCATION'}</option>
          <option value={'android.permission.ACCESS_COARSE_LOCATION'}>{'ACCESS_COARSE_LOCATION'}</option>
        </select>
      </p>

      <button
        onClick={() => {
          permissionsManager.askForPermission(permissions).then(setStatuses);
        }}
      >
        {'Request permissions'}
      </button>

      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(statuses, null, 2)}</pre>
    </>
  );
}
