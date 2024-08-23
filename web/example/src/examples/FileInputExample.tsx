import React, { useState } from 'react';
import { Section } from '../components/Section';
import { Checkbox, Select, SelectOption } from '../components/Select';

export function FileInputExample() {
  const [files, setFiles] = useState<File[]>([]);
  const [accept, setAccept] = useState(['*/*']);
  const [multiple, setMultiple] = useState(false);

  return (
    <Section title={'File input'}>
      <label className="form-label">{'Accept'}</label>
      <Select
        values={accept}
        onChange={values => {
          setAccept(values);
        }}
        multiple={true}
      >
        <SelectOption value={'image/*'}>{'image/*'}</SelectOption>
        <SelectOption value={'video/*'}>{'video/*'}</SelectOption>
        <SelectOption value={'text/html'}>{'text/html'}</SelectOption>
        <SelectOption value={'*/*'}>{'*/*'}</SelectOption>
      </Select>

      <Checkbox
        checked={multiple}
        onChange={event => {
          setMultiple(event.target.checked);
        }}
      >
        {'Multiple'}
      </Checkbox>

      <input
        className="form-control mt-3"
        type="file"
        accept={accept.length === 0 ? undefined : accept.join(',')}
        multiple={multiple}
        onChange={event => {
          setFiles(event.target.files === null ? [] : Array.from(event.target.files));
        }}
      />

      <ol>
        {files.map((file, index) => (
          <li key={index}>{file.name + ' (' + file.type + ')'}</li>
        ))}
      </ol>

      <button
        className="btn btn-secondary mt-3"
        onClick={() => {
          Promise.allSettled(files.map(readFileAsBase64)).then(results => {
            console.log(results.map(result => (result.status === 'fulfilled' ? result.value : null)));
          });
        }}
      >
        {'Print contents to console'}
      </button>
    </Section>
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
