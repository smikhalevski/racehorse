import React, { useState } from 'react';
import { encryptedStorageManager } from 'racehorse';

export function EncryptedStorageExample() {
  const [value, setValue] = useState('test');
  const [password, setPassword] = useState('test');
  const [persistedValue, setPersistedValue] = useState<string | null>(null);

  return (
    <>
      <h2>{'Encrypted storage'}</h2>
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
            encryptedStorageManager.set('test', value, password);
          }}
        >
          {'Set'}
        </button>{' '}
        <button
          onClick={() => {
            encryptedStorageManager.delete('test');
          }}
        >
          {'Delete'}
        </button>
      </p>

      {'Value: '}
      <pre>{JSON.stringify(persistedValue)}</pre>

      <button
        onClick={() => {
          encryptedStorageManager.get('test', password).then(setPersistedValue);
        }}
      >
        {'Get'}
      </button>
    </>
  );
}
