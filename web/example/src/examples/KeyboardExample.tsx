import React from 'react';
import { useKeyboardHeight } from '@racehorse/react';
import { keyboardManager } from 'racehorse';

export function KeyboardExample() {
  const keyboardHeight = useKeyboardHeight();

  return (
    <>
      <h2>{'Keyboard'}</h2>

      <button onClick={keyboardManager.showKeyboard}>{'Show keyboard'}</button>

      <p>{'Keyboard height: ' + keyboardHeight}</p>
    </>
  );
}
