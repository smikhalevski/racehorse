import React, { useState } from 'react';
import { permissionsManager } from 'racehorse';
import { Select, SelectOption } from '../components/Select';
import { Heading } from '../components/Heading';

const samplePermissions = [
  'android.permission.ACCESS_WIFI_STATE',
  'android.permission.ACCESS_NETWORK_STATE',
  'android.permission.CHANGE_WIFI_STATE',
  'android.permission.CALL_PHONE',
  'android.permission.READ_EXTERNAL_STORAGE',
  'android.permission.CAMERA',
  'android.permission.ACCESS_FINE_LOCATION',
  'android.permission.ACCESS_COARSE_LOCATION',
];

export function PermissionsExample() {
  const [permissions, setPermissions] = useState<string[]>([]);
  const [statuses, setStatuses] = useState<{ [permission: string]: boolean }>({});

  return (
    <>
      <Heading>{'Permissions'}</Heading>

      <Select
        values={permissions}
        onChange={setPermissions}
        isMultiple={true}
      >
        <ul className="list-group mb-3">
          {samplePermissions.map(permission => (
            <li className="list-group-item d-flex">
              <SelectOption value={permission}>{permission.split('.').pop()}</SelectOption>

              {permission in statuses && (
                <i
                  className={
                    statuses[permission]
                      ? 'ms-auto bi-check-circle-fill text-success'
                      : // https://developer.android.com/training/permissions/requesting#explain
                        permissionsManager.shouldShowRequestPermissionRationale(permission)
                        ? 'ms-auto bi-question-circle-fill text-warning'
                        : 'ms-auto bi-x-circle-fill text-danger'
                  }
                />
              )}
            </li>
          ))}
        </ul>
      </Select>

      <button
        className="btn btn-primary d-block mx-auto"
        disabled={permissions.length === 0}
        onClick={() => {
          permissionsManager.askForPermission(permissions).then(setStatuses);
        }}
      >
        {'Request permissions'}
      </button>
    </>
  );
}
