import React, { useReducer } from 'react';
import { FormattedJSON } from '../components/FormattedJSON';

export function CookieExample() {
  const [, rerender] = useReducer(value => ++value, 0);

  return (
    <>
      <h2>{'Cookie'}</h2>

      <p>
        <button
          onClick={() => {
            document.cookie =
              'example=Hello, world!; expires=' + new Date(Date.now() + 24 * 60 * 60 * 1000).toUTCString();
            rerender();
          }}
        >
          {'Set value'}
        </button>{' '}
        <button
          onClick={() => {
            document.cookie =
              'example=Hello, world!; expires=' + new Date(Date.now() - 24 * 60 * 60 * 1000).toUTCString();
            rerender();
          }}
        >
          {'❌ Delete value'}
        </button>
      </p>

      {'Value:'}
      <FormattedJSON value={document.cookie} />
    </>
  );
}
