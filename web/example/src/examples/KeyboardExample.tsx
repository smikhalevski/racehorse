import React, { useEffect, useState } from 'react';
import { keyboardManager } from 'racehorse';

export function KeyboardExample() {
  const [keyboardHeight, setKeyboardHeight] = useState(keyboardManager.getKeyboardHeight);

  useEffect(() => keyboardManager.subscribe('toggled', setKeyboardHeight), []);

  return (
    <>
      <h2>{'Keyboard'}</h2>

      <input placeholder={'Set focus here'} />

      <p>{'Keyboard height: ' + keyboardHeight}</p>
    </>
  );
}
