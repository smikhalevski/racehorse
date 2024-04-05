import React, { useState } from 'react';

export function FileInputExample() {
  const [files, setFiles] = useState<File[]>([]);
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

      <p>
        <input
          type="file"
          accept={accept}
          multiple={multiple}
          onChange={event => {
            setFiles(event.target.files !== null ? Array.from(event.target.files) : []);
          }}
        />
      </p>

      <ol>
        {files.map((file, index) => (
          <li key={index}>{file.name + ' (' + file.type + ')'})</li>
        ))}
      </ol>
    </>
  );
}
