import React, { useState } from 'react';
import { encryptedStorageManager } from 'racehorse';

export function EncryptedStorageExample() {
  const [key, setKey] = useState('my_key');
  const [value, setValue] = useState('my_value');
  const [password, setPassword] = useState('my_password');
  const [storedValueInfo, setStoredValueInfo] = useState('UNKNOWN');

  return (
    <>
      <h2>{'Encrypted storage'}</h2>

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
            encryptedStorageManager.get(key, password).then(value => {
              setStoredValueInfo('GET ' + JSON.stringify(value));
            });
          }}
        >
          {'Get value'}
        </button>{' '}
        <button
          onClick={() => {
            encryptedStorageManager.delete(key);

            setStoredValueInfo('DELETE null');
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
            const nextValue = value;
            encryptedStorageManager.set(key, nextValue, password).then(isSuccessful => {
              setStoredValueInfo('SET ' + (isSuccessful ? JSON.stringify(nextValue) : '💥Failed'));
            });
          }}
        >
          {'Set value'}
        </button>
      </p>
    </>
  );
}
