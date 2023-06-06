import React, { useState } from 'react';
import { eventBridge } from 'racehorse';

export function EventBridgeExample() {
  const [eventType, setEventType] = useState('org.racehorse.GetKeyboardStatusEvent');

  return (
    <>
      <h2>{'EventBridge'}</h2>

      <p>
        {'Is '}
        <input
          value={eventType}
          onChange={event => {
            setEventType(event.target.value);
          }}
        />
        {' supported? '}
      </p>

      <p>{eventBridge.isSupported(eventType) ? '✅ Yes' : '❌ No'}</p>
    </>
  );
}
