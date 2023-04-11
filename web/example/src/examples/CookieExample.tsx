import React, { useReducer } from 'react';

export function CookieExample() {
  const [, rerender] = useReducer(value => ++value, 0);

  return (
    <>
      <h2>{'Cookies'}</h2>

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
          {'Clear value'}
        </button>
      </p>

      {'Value: '}
      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(document.cookie, null, 2)}</pre>
    </>
  );
}
