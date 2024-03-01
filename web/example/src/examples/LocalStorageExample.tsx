import React, { useReducer } from 'react';
import { FormattedJSON } from '../components/FormattedJSON';

export function LocalStorageExample() {
  const [, rerender] = useReducer(value => ++value, 0);

  return (
    <>
      <h2>{'Local storage'}</h2>

      <p>
        <button
          onClick={() => {
            localStorage.setItem('example', 'Hello, world!');
            rerender();
          }}
        >
          {'Set value'}
        </button>{' '}
        <button
          onClick={() => {
            localStorage.removeItem('example');
            rerender();
          }}
        >
          {'❌ Delete value'}
        </button>
      </p>

      {'Value:'}
      <FormattedJSON value={localStorage.getItem('example')} />
    </>
  );
}
