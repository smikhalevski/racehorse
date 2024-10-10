import React, { useState } from 'react';
import { eventBridge } from 'racehorse';
import { Section } from '../components/Section';

export function ToastExample() {
  const [message, setMessage] = useState('Hello');

  return (
    <Section title={'Toast'}>
      <div className="input-group">
        <input
          className="form-control"
          value={message}
          onChange={event => setMessage(event.target.value)}
        />

        <button
          className="btn btn-primary"
          onClick={() => {
            eventBridge.request({ type: 'com.example.ShowToastEvent', payload: { message } });
          }}
        >
          {'Show toast'}
        </button>
      </div>
    </Section>
  );
}
