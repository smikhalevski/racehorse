import React, { useState } from 'react';
import { BiometricAuthenticator, biometricEncryptedStorageManager } from 'racehorse';

export function BiometricEncryptedStorageExample() {
  const [key, setKey] = useState('my_key');
  const [value, setValue] = useState('my_value');
  const [authenticationValidityDuration, setAuthenticationValidityDuration] = useState(-1);
  const [authenticators, setAuthenticators] = useState<BiometricAuthenticator[]>([
    BiometricAuthenticator.BIOMETRIC_STRONG,
  ]);
  const [storedValueInfo, setstoredValueInfo] = useState('UNKNOWN');

  return (
    <>
      <h2>{'Biometric encrypted storage'}</h2>

      {'Stored value: '}
      <strong>{storedValueInfo}</strong>

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
                negativeButtonText: 'Cancel get',
                authenticators,
              })
              .then(result => {
                setstoredValueInfo(
                  'GET ' + (result.errorCode === null ? JSON.stringify(result.value) : '💥 Error ' + result.errorCode)
                );
              });
          }}
        >
          {'Get value'}
        </button>{' '}
        <button
          onClick={() => {
            biometricEncryptedStorageManager.delete(key);

            setstoredValueInfo('DELETE null');
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
            const nextValue = value;

            biometricEncryptedStorageManager
              .set(key, nextValue, {
                title: 'Set value to storage',
                negativeButtonText: 'Cancel set',
                authenticators,
                authenticationValidityDuration,
              })
              .then(result => {
                setstoredValueInfo(
                  'SET ' + (result.isSuccessful ? JSON.stringify(nextValue) : '💥 Error ' + result.errorCode)
                );
              });
          }}
        >
          {'Set value'}
        </button>
      </p>

      <p>
        {'Authenticators: '}
        <br />
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
        {'Authentication validity duration: '}
        <br />
        <em>{'Applied only if the key is set for the first time.'}</em>
      </p>

      <p>
        <input
          type="range"
          value={authenticationValidityDuration}
          min={-1}
          max={10}
          step={1}
          onChange={event => {
            setAuthenticationValidityDuration(event.target.valueAsNumber);
          }}
        />{' '}
        {authenticationValidityDuration === -1 ? 'Unset' : authenticationValidityDuration + ' seconds'}
      </p>
    </>
  );
}
