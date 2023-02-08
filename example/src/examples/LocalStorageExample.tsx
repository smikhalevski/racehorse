import React, { useReducer } from 'react';

module.hot?.accept(() => {
  location.reload();
});

export function LocalStorageExample() {
  const [, rerender] = useReducer(value => ++value, 0);

  return (
    <>
      <h2>{'LocalStorageExample'}</h2>

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
          {'Clear value'}
        </button>
      </p>

      {'Value: '}
      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(localStorage.getItem('example'), null, 2)}</pre>
    </>
  );
}
