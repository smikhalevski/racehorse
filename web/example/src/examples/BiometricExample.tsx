import React, { useMemo, useState } from 'react';
import { BiometricAuthenticator, biometricManager, BiometricStatus } from 'racehorse';

export function BiometricExample() {
  const [authenticators, setAuthenticators] = useState<BiometricAuthenticator[]>([
    BiometricAuthenticator.BIOMETRIC_STRONG,
  ]);

  const status = useMemo(() => biometricManager.getBiometricStatus(authenticators), [authenticators]);

  return (
    <>
      <h2>{'Biometric'}</h2>

      <p>
        {'Authenticators: '}
        <select
          className="form-select"
          multiple={true}
          value={authenticators}
          onChange={event => {
            setAuthenticators(
              Array.from(event.target.selectedOptions).map(option => option.value as BiometricAuthenticator)
            );
          }}
        >
          <option value={BiometricAuthenticator.BIOMETRIC_STRONG}>{'Biometric strong'}</option>
          <option value={BiometricAuthenticator.BIOMETRIC_WEAK}>{'Biometric weak'}</option>
          <option value={BiometricAuthenticator.DEVICE_CREDENTIAL}>{'Device credential'}</option>
        </select>
      </p>

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

      {status === BiometricStatus.NONE_ENROLLED && (
        <p>
          <button
            className="btn btn-primary"
            onClick={() => {
              biometricManager.enrollBiometric(authenticators).then(() => {
                // Trigger status update
                setAuthenticators(authenticators => [...authenticators]);
              });
            }}
          >
            {'Enroll biometric'}
          </button>
        </p>
      )}
    </>
  );
}
