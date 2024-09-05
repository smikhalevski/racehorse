import React from 'react';
import { useKeyboardHeight } from '@racehorse/react';
import { keyboardManager } from 'racehorse';

export function KeyboardExample() {
  const keyboardHeight = useKeyboardHeight();

  return (
    <Section title={'Keyboard'}>
      <button onClick={keyboardManager.showKeyboard}>{'Show keyboard'}</button>

      <p>{'Keyboard height: ' + keyboardHeight}</p>
    </Section>
  );
}
