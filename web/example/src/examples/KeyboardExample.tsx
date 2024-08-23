import React from 'react';
import { useKeyboardHeight } from '@racehorse/react';
import { keyboardManager } from 'racehorse';

export function KeyboardExample() {
  const keyboardHeight = useKeyboardHeight();

  return (
    <>
      <h1>{'Keyboard'}</h1>

      <button onClick={keyboardManager.showKeyboard}>{'Show keyboard'}</button>

      <p>{'Keyboard height: ' + keyboardHeight}</p>
    </>
  );
}
