import React, { useMemo, useState } from 'react';
import { BiometricAuthenticator, biometricManager, BiometricStatus } from 'racehorse';
import { Select, SelectOption } from '../components/Select';
import { Section } from '../components/Section';

export function BiometricExample() {
  const [authenticators, setAuthenticators] = useState<BiometricAuthenticator[]>([
    BiometricAuthenticator.BIOMETRIC_STRONG,
  ]);

  const status = useMemo(() => biometricManager.getBiometricStatus(authenticators), [authenticators]);

  return (
    <Section title={'Biometric'}>
      <i
        className={
          status === BiometricStatus.SUPPORTED
            ? 'bi-check-circle-fill text-success me-2'
            : 'bi-x-circle-fill text-danger me-2'
        }
      />

      {
        {
          [BiometricStatus.SUPPORTED]: 'Supported',
          [BiometricStatus.UNKNOWN]: 'Unknown',
          [BiometricStatus.UNSUPPORTED]: 'Unsupported',
          [BiometricStatus.NO_HARDWARE]: 'No hardware',
          [BiometricStatus.HARDWARE_UNAVAILABLE]: 'Hardware unavailable',
          [BiometricStatus.NONE_ENROLLED]: 'None enrolled',
          [BiometricStatus.SECURITY_UPDATE_REQUIRED]: 'Security update required',
        }[status]
      }

      <label className="form-label d-block mt-3">{'Authenticator'}</label>
      <Select
        values={authenticators}
        onChange={setAuthenticators}
        isMultiple={true}
      >
        <SelectOption value={BiometricAuthenticator.BIOMETRIC_STRONG}>{'Biometric strong'}</SelectOption>
        <SelectOption value={BiometricAuthenticator.BIOMETRIC_WEAK}>{'Biometric weak'}</SelectOption>
        <SelectOption value={BiometricAuthenticator.DEVICE_CREDENTIAL}>{'Device credential'}</SelectOption>
      </Select>

      <button
        className="btn d-block w-100 btn-primary mt-3"
        onClick={() => {
          biometricManager.enrollBiometric(authenticators).then(() => {
            // Trigger status update
            setAuthenticators(authenticators => [...authenticators]);
          });
        }}
      >
        {'Enroll biometric'}
      </button>
    </Section>
  );
}
