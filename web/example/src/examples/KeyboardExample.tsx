import React from 'react';
import { FormattedJSON } from '../components/FormattedJSON';
import { useKeyboardStatus } from '@racehorse/react';

export function KeyboardExample() {
  const keyboardStatus = useKeyboardStatus();

  return (
    <>
      <h2>{'Keyboard'}</h2>

      <input placeholder={'Set focus here'} />

      <p>
        {'Status: '}
        <FormattedJSON value={keyboardStatus} />
      </p>
    </>
  );
}
