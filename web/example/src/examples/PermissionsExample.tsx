import React, { useMemo, useState } from 'react';
import { permissionsManager } from 'racehorse';
import { Select, SelectOption } from '../components/Select';
import { Section } from '../components/Section';

const examplePermissions = [
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
  const [grantedStatuses, setGrantedStatuses] = useState<{ [permission: string]: boolean }>({});

  // https://developer.android.com/training/permissions/requesting#explain
  const rationaleStatuses = useMemo(
    () => permissionsManager.shouldShowRequestPermissionRationale(permissions),
    [grantedStatuses]
  );

  return (
    <Section title={'Permissions'}>
      <Select
        values={permissions}
        onChange={setPermissions}
        multiple={true}
      >
        {examplePermissions.map(permission => (
          <SelectOption
            key={permission}
            value={permission}
          >
            <div className="d-flex justify-content-between">
              {permission.split('.').pop()}

              {permission in grantedStatuses && (
                <i
                  className={
                    grantedStatuses[permission]
                      ? 'bi-check-circle-fill text-success'
                      : rationaleStatuses[permission]
                        ? 'bi-question-circle-fill text-warning'
                        : 'bi-x-circle-fill text-danger'
                  }
                />
              )}
            </div>
          </SelectOption>
        ))}
      </Select>

      <button
        className="btn btn-primary w-100 mt-3"
        disabled={permissions.length === 0}
        onClick={() => {
          permissionsManager.askForPermission(permissions).then(setGrantedStatuses);
        }}
      >
        {'Request permissions'}
      </button>
    </Section>
  );
}
