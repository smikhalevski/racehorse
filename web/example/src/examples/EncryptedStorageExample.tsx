import React, { useEffect, useState } from 'react';
import { encryptedStorageManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';

export function EncryptedStorageExample() {
  const [key, setKey] = useState('my_key');
  const [value, setValue] = useState('my_value');
  const [password, setPassword] = useState('my_password');
  const [persistedValue, setPersistedValue] = useState<string | null>(null);

  useEffect(() => {
    encryptedStorageManager.get(key, password).then(setPersistedValue);
  }, [key, value, password]);

  return (
    <>
      <h2>{'Encrypted storage'}</h2>

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

        <div>
          {'Password:'}
          <br />
          <input
            type="text"
            value={password}
            size={10}
            onChange={event => {
              setPassword(event.target.value);
            }}
          />
        </div>
      </div>

      <p>
        <button
          onClick={() => {
            encryptedStorageManager.set(key, value, password).then(isSuccessful => {
              if (isSuccessful) {
                encryptedStorageManager.get(key, password).then(setPersistedValue);
              }
            });
          }}
        >
          {'Set value'}
        </button>{' '}
        <button
          onClick={() => {
            if (encryptedStorageManager.delete(key)) {
              encryptedStorageManager.get(key, password).then(setPersistedValue);
            }
          }}
        >
          {'Delete value'}
        </button>
      </p>

      {'Persisted value: '}
      <FormattedJSON value={persistedValue} />
    </>
  );
}
