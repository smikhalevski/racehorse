import React, { useState } from 'react';
import { eventBridge } from 'racehorse';

export function ToastExample() {
  const [message, setMessage] = useState('Hello');

  return (
    <>
      <h2>{'Toast'}</h2>

      <p>
        <label className="form-label">{'Message'}</label>
        <input
          className="form-control"
          value={message}
          onChange={event => {
            setMessage(event.target.value);
          }}
        />
      </p>

      <button
        className="btn btn-primary"
        onClick={() => {
          eventBridge.request({ type: 'com.example.ShowToastEvent', payload: { message } });
        }}
      >
        {'Show toast'}
      </button>
    </>
  );
}
