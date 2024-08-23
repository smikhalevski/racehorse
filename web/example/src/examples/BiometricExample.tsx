import React, { useMemo, useState } from 'react';
import { BiometricAuthenticator, biometricManager, BiometricStatus } from 'racehorse';
import { Select, SelectOption } from '../components/Select';

export function BiometricExample() {
  const [authenticators, setAuthenticators] = useState<BiometricAuthenticator[]>([
    BiometricAuthenticator.BIOMETRIC_STRONG,
  ]);

  const status = useMemo(() => biometricManager.getBiometricStatus(authenticators), [authenticators]);

  return (
    <>
      <h1>{'Biometric'}</h1>

      <ul className="list-group mb-3">
        <Select
          values={authenticators}
          onChange={setAuthenticators}
          multiple={true}
        >
          <li className="list-group-item">
            <SelectOption value={BiometricAuthenticator.BIOMETRIC_STRONG}>{'Biometric strong'}</SelectOption>
          </li>
          <li className="list-group-item">
            <SelectOption value={BiometricAuthenticator.BIOMETRIC_WEAK}>{'Biometric weak'}</SelectOption>
          </li>
          <li className="list-group-item">
            <SelectOption value={BiometricAuthenticator.DEVICE_CREDENTIAL}>{'Device credential'}</SelectOption>
          </li>
        </Select>
      </ul>

      <div className="mb-3">
        {
          {
            [BiometricStatus.SUPPORTED]: '✅ Supported',
            [BiometricStatus.UNKNOWN]: '❌ Unknown',
            [BiometricStatus.UNSUPPORTED]: '❌ Unsupported',
            [BiometricStatus.NO_HARDWARE]: '❌ No hardware',
            [BiometricStatus.HARDWARE_UNAVAILABLE]: '❌ Hardware unavailable',
            [BiometricStatus.NONE_ENROLLED]: '❌ None enrolled',
            [BiometricStatus.SECURITY_UPDATE_REQUIRED]: '❌ Security update required',
          }[status]
        }
      </div>

      {status === BiometricStatus.NONE_ENROLLED && (
        <button
          className="btn d-block w-100 btn-primary"
          onClick={() => {
            biometricManager.enrollBiometric(authenticators).then(() => {
              // Trigger status update
              setAuthenticators(authenticators => [...authenticators]);
            });
          }}
        >
          {'Enroll biometric'}
        </button>
      )}
    </>
  );
}
