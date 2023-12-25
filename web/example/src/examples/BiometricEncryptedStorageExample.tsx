import React, { useEffect, useState } from 'react';
import { FormattedJSON } from '../components/FormattedJSON';
import { BiometricAuthenticator, BiometricConfig, biometricEncryptedStorageManager } from 'racehorse';

const biometricConfig: BiometricConfig = {
  title: 'Authentication required',
  authenticators: [BiometricAuthenticator.BIOMETRIC_STRONG],
};

export function BiometricEncryptedStorageExample() {
  const [key, setKey] = useState('my_key');
  const [value, setValue] = useState('my_value');
  const [persistedValue, setPersistedValue] = useState<string | null>(null);

  useEffect(() => {
    biometricEncryptedStorageManager.get(key).then(setPersistedValue);
  }, []);

  return (
    <>
      <h2>{'Biometric encrypted storage'}</h2>

      <div style={{ display: 'flex', gap: '10px' }}>
        <div>
          {'Key:'}
          <br />
          <input
            type="text"
            value={key}
            size={10}
            onChange={event => {
              setKey(event.target.value);
            }}
          />
        </div>

        <div>
          {'Value:'}
          <br />
          <input
            type="text"
            value={value}
            size={10}
            onChange={event => {
              setValue(event.target.value);
            }}
          />
        </div>
      </div>

      <p>
        <button
          onClick={() => {
            biometricEncryptedStorageManager.get(key).then(setPersistedValue);
          }}
        >
          {'Get'}
        </button>{' '}
        <button
          onClick={() => {
            biometricEncryptedStorageManager.set(key, value, biometricConfig).then(ok => {
              if (ok) {
                biometricEncryptedStorageManager.get(key, biometricConfig).then(setPersistedValue);
              } else {
                console.log('Cannot set biometric storage entry: ' + key);
              }
            });
          }}
        >
          {'Set'}
        </button>{' '}
        <button
          onClick={() => {
            if (biometricEncryptedStorageManager.delete(key)) {
              biometricEncryptedStorageManager.get(key, biometricConfig).then(setPersistedValue);
            }
          }}
        >
          {'Delete'}
        </button>
      </p>

      {'Persisted value: '}
      <FormattedJSON value={persistedValue} />
    </>
  );
}
