import React, { useState } from 'react';

module.hot?.accept(() => {
  location.reload();
});

export function FileInputExample() {
  const [accept, setAccept] = useState('image/*');
  const [multiple, setMultiple] = useState(false);

  return (
    <>
      <h2>{'FileInputExample'}</h2>

      <p>
        {'Accept: '}
        <select
          onChange={event => {
            setAccept(event.target.value);
          }}
        >
          <option value={'image/*'}>{'image/*'}</option>
          <option value={'video/*'}>{'video/*'}</option>
          <option value={'image/*,video/*'}>{'image/*, video/*'}</option>
          <option value={'text/html'}>{'text/html'}</option>
        </select>
      </p>

      <p>
        {'Multiple: '}
        <input
          type="checkbox"
          checked={multiple}
          onChange={event => setMultiple(event.target.checked)}
        />
      </p>

      <input
        type="file"
        accept={accept}
        multiple={multiple}
      />
    </>
  );
}
