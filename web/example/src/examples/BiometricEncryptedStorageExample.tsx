import React, { useState } from 'react';
import { FormattedJSON } from '../components/FormattedJSON.js';
import { BiometricAuthenticator, biometricEncryptedStorageManager } from 'racehorse';

export function BiometricEncryptedStorageExample() {
  const [key, setKey] = useState('my_key');
  const [value, setValue] = useState('my_value');
  const [authenticationValidityDuration, setAuthenticationValidityDuration] = useState(-1);
  const [authenticators, setAuthenticators] = useState<BiometricAuthenticator[]>([
    BiometricAuthenticator.BIOMETRIC_STRONG,
  ]);
  const [storedValue, setStoredValue] = useState<string | null>(null);

  return (
    <>
      <h2>{'Biometric encrypted storage'}</h2>

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

      <p>
        {'Authentication validity duration: ' +
          (authenticationValidityDuration === -1 ? 'Disabled' : authenticationValidityDuration + ' seconds')}
        <br />
        <input
          type="range"
          value={authenticationValidityDuration}
          min={-1}
          max={10}
          step={1}
          style={{ margin: '0.5rem 3rem 0', width: 'calc(100% - 6rem)' }}
          onChange={event => {
            setAuthenticationValidityDuration(event.target.valueAsNumber);
          }}
        />
      </p>

      <p>
        {'Key:'}
        <br />
        <input
          type="text"
          value={key}
          size={10}
          onChange={event => {
            setKey(event.target.value);
          }}
        />{' '}
        <button
          onClick={() => {
            biometricEncryptedStorageManager
              .get(key, {
                title: 'Get value from storage',
                authenticators,
              })
              .then(setStoredValue);
          }}
        >
          {'Get value'}
        </button>{' '}
        <button
          onClick={() => {
            if (biometricEncryptedStorageManager.delete(key)) {
              setStoredValue(null);
            }
          }}
        >
          {'❌ Delete key'}
        </button>
      </p>

      <p>
        {'Value:'}
        <br />
        <input
          type="text"
          value={value}
          size={10}
          onChange={event => {
            setValue(event.target.value);
          }}
        />{' '}
        <button
          onClick={() => {
            biometricEncryptedStorageManager
              .set(key, value, {
                title: 'Set value to storage',
                authenticators,
                authenticationValidityDuration,
              })
              .then(isSuccessful => {
                if (!isSuccessful) {
                  console.log('❌ Cannot set biometric storage entry: ' + key);
                }
              });
          }}
        >
          {'Set value'}
        </button>
      </p>

      {'Stored value: '}
      <FormattedJSON value={storedValue} />
    </>
  );
}
