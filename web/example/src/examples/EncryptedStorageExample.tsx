import React, { useEffect, useState } from 'react';
import { encryptedStorageManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';

export function EncryptedStorageExample() {
  const [key, setKey] = useState('my_key');
  const [value, setValue] = useState('my_value');
  const [password, setPassword] = useState('my_password');
  const [storedValue, setStoredValue] = useState<string | null>(null);

  useEffect(() => {
    encryptedStorageManager.get(key, password).then(setStoredValue);
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
          size={10}
          onChange={event => {
            setKey(event.target.value);
          }}
        />{' '}
        <button
          onClick={() => {
            if (encryptedStorageManager.delete(key)) {
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
        />
      </p>
      <p>
        {'Password:'}
        <br />
        <input
          type="text"
          value={password}
          size={10}
          onChange={event => {
            setPassword(event.target.value);
          }}
        />{' '}
        <button
          onClick={() => {
            encryptedStorageManager.set(key, value, password).then(isSuccessful => {
              if (isSuccessful) {
                encryptedStorageManager.get(key, password).then(setStoredValue);
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
