import React, { useState } from 'react';

export function FileInputExample() {
  const [accept, setAccept] = useState('*/*');
  const [multiple, setMultiple] = useState(false);

  return (
    <>
      <h2>{'File input'}</h2>

      <p>
        {'Accept: '}
        <select
          value={accept}
          onChange={event => {
            setAccept(event.target.value);
          }}
        >
          <option value={'image/*'}>{'image/*'}</option>
          <option value={'video/*'}>{'video/*'}</option>
          <option value={'image/*,video/*'}>{'image/*, video/*'}</option>
          <option value={'text/html'}>{'text/html'}</option>
          <option value={'*/*'}>{'*/*'}</option>
        </select>
      </p>

      <p>
        {'Multiple: '}
        <input
          type="checkbox"
          checked={multiple}
          onChange={event => {
            setMultiple(event.target.checked);
          }}
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
