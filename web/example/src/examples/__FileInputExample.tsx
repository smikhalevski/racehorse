import React, { useState } from 'react';
import { Section } from '../components/Section';
import { Select, SelectOption } from '../components/Select';
import { Checkbox } from '../components/Checkbox';

export function FileInputExample() {
  const [files, setFiles] = useState<File[]>([]);
  const [accept, setAccept] = useState(['*/*']);
  const [isMultiple, setMultiple] = useState(false);

  return (
    <Section title={'File input'}>
      <label className="form-label">{'Accept'}</label>
      <Select
        values={accept}
        onChange={setAccept}
      >
        <SelectOption value={'image/*'}>{'image/*'}</SelectOption>
        <SelectOption value={'video/*'}>{'video/*'}</SelectOption>
        <SelectOption value={'text/html'}>{'text/html'}</SelectOption>
        <SelectOption value={'*/*'}>{'*/*'}</SelectOption>
      </Select>

      <Checkbox
        className="mt-3"
        checked={isMultiple}
        onChange={event => setMultiple(event.target.checked)}
      >
        {'Multiple'}
      </Checkbox>

      <input
        className="form-control mt-3"
        type="file"
        accept={accept.length === 0 ? undefined : accept.join(',')}
        multiple={isMultiple}
        onChange={event => setFiles(event.target.files === null ? [] : Array.from(event.target.files))}
      />

      <ul className="list-group mt-3">
        {files.length === 0 && (
          <li className="list-group-item list-group-item-light text-secondary text-center">{'No files'}</li>
        )}

        {files.map((file, index) => (
          <li
            key={index}
            className="list-group-item"
          >
            {file.name}
            <small className="d-block text-body-secondary">{file.type}</small>
          </li>
        ))}
      </ul>
    </Section>
  );
}
