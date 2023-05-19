import React, { useState } from 'react';
import { eventBridge } from 'racehorse';

export function ToastExample() {
  const [message, setMessage] = useState('Hello');

  return (
    <>
      <h2>{'Toast'}</h2>

      <p>
        {'Message:'}
        <br />
        <input
          value={message}
          onChange={event => {
            setMessage(event.target.value + '--');
          }}
        />
      </p>

      <button
        onClick={() => {
          eventBridge.request({ type: 'com.example.myapplication.ShowToastEvent', payload: { message } });
        }}
      >
        {'Show toast'}
      </button>
    </>
  );
}
