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
      <p>
        {'Key:'}
        <br />
        <input
          type="text"
          value={key}
          onChange={event => {
            setKey(event.target.value);
          }}
        />
      </p>

      <p>
        {'Value:'}
        <br />
        <input
          type="text"
          value={value}
          onChange={event => {
            setValue(event.target.value);
          }}
        />
      </p>

      <p>
        {'Password:'}
        <br />
        <input
          type="text"
          value={password}
          onChange={event => {
            setPassword(event.target.value);
          }}
        />
      </p>

      <p>
        <button
          onClick={() => {
            encryptedStorageManager.set(key, value, password).then(() => {
              encryptedStorageManager.get(key, password).then(setPersistedValue);
            });
          }}
        >
          {'Set value'}
        </button>{' '}
        <button
          onClick={() => {
            encryptedStorageManager.delete(key);
            encryptedStorageManager.get(key, password).then(() => {
              encryptedStorageManager.get(key, password).then(setPersistedValue);
            });
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
