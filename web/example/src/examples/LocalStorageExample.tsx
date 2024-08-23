import React, { useReducer } from 'react';
import { FormattedJSON } from '../components/FormattedJSON';

export function LocalStorageExample() {
  const [, rerender] = useReducer(value => ++value, 0);

  return (
    <>
      <h1>{'Local storage'}</h1>

      <p>
        <button
          className="btn btn-primary"
          onClick={() => {
            localStorage.setItem('example', 'Hello, world!');
            rerender();
          }}
        >
          {'Set value'}
        </button>{' '}
        <button
          className="btn btn-primary"
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
