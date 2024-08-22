import React, { useState } from 'react';

export function FileInputExample() {
  const [files, setFiles] = useState<File[]>([]);
  const [accept, setAccept] = useState('*/*');
  const [multiple, setMultiple] = useState(false);

  return (
    <>
      <h2>{'File input'}</h2>

      <p>
        <label className="form-label">{'Accept'}</label>
        <select
          className="form-select"
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
        <div className="form-check">
          <input
            className="form-check-input"
            type="checkbox"
            checked={multiple}
            onChange={event => {
              setMultiple(event.target.checked);
            }}
          />
          <label className="form-check-label">{'Multiple'}</label>
        </div>
      </p>

      <p>
        <input
          className="form-control"
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
          <li key={index}>{file.name + ' (' + file.type + ')'}</li>
        ))}
      </ol>

      <p>
        <button
          className="btn btn-secondary"
          onClick={() => {
            Promise.allSettled(files.map(readFileAsBase64)).then(results => {
              console.log(results.map(result => (result.status === 'fulfilled' ? result.value : null)));
            });
          }}
        >
          {'Print contents to console'}
        </button>
      </p>
    </>
  );
}

function readFileAsBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const fileReader = new FileReader();

    fileReader.addEventListener('loadend', () => {
      resolve(fileReader.result as string);
    });
    fileReader.addEventListener('error', () => {
      reject(new Error('Failed to read file'));
    });
    fileReader.readAsDataURL(file);
  });
}
