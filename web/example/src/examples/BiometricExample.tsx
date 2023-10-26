import React, { useMemo, useState } from 'react';
import { BiometricAuthenticator, biometricManager, BiometricStatus } from 'racehorse';

export function BiometricExample() {
  const [authenticators, setAuthenticators] = useState<BiometricAuthenticator[]>([
    BiometricAuthenticator.BIOMETRIC_WEAK,
  ]);

  const status = useMemo(() => biometricManager.getBiometricStatus(authenticators), [authenticators]);

  return (
    <>
      <h2>{'Biometric'}</h2>

      <p>
        {'Authenticators: '}
        <select
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
          [BiometricStatus.NO_HARDWARE]: '❌ No hardware',
          [BiometricStatus.HARDWARE_UNAVAILABLE]: '❌ Hardware unavailable',
          [BiometricStatus.NONE_ENROLLED]: '❌ None enrolled',
        }[status]
      }

      {status === BiometricStatus.NONE_ENROLLED && (
        <p>
          <button
            onClick={() => {
              biometricManager
                .enrollBiometric(authenticators)
                // Trigger status update
                .then(() => setAuthenticators(authenticators => [...authenticators]));
            }}
          >
            {'Enroll biometric'}
          </button>
        </p>
      )}
    </>
  );
}
