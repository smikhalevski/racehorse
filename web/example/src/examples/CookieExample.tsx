import React, { useReducer } from 'react';
import { FormattedJSON } from '../components/FormattedJSON';

export function CookieExample() {
  const [, rerender] = useReducer(value => ++value, 0);

  return (
    <>
      <h1>{'Cookie'}</h1>

      <p>
        <button
          className="btn btn-primary"
          onClick={() => {
            document.cookie =
              'example=Hello, world!; expires=' + new Date(Date.now() + 24 * 60 * 60 * 1000).toUTCString();
            rerender();
          }}
        >
          {'Set value'}
        </button>{' '}
        <button
          className="btn btn-primary"
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
