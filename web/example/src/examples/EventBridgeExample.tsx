import React, { useState } from 'react';
import { eventBridge } from 'racehorse';

export function EventBridgeExample() {
  const [eventType, setEventType] = useState('org.racehorse.IsSupportedEvent');

  return (
    <>
      <h2>{'Event bridge'}</h2>

      <p>
        <label className="form-label">{'Event name'}</label>
        <input
          className="form-control font-monospace"
          value={eventType}
          onChange={event => {
            setEventType(event.target.value);
          }}
        />
      </p>

      {eventBridge.isSupported(eventType) ? (
        <>
          <i className="bi-check-circle-fill text-success me-2" />
          {'Supported'}
        </>
      ) : (
        <>
          <i className="bi-x-circle-fill text-success me-2" />
          {'Not supported'}
        </>
      )}
    </>
  );
}
