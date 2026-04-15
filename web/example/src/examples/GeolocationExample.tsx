import React, { useState } from 'react';
import { FormattedJSON } from '../components/FormattedJSON.js';

export function GeolocationExample() {
  const [geolocation, setGeolocation] = useState<unknown>();

  return (
    <>
      <h2>{'Geolocation'}</h2>

      <button
        onClick={() => {
          navigator.geolocation.getCurrentPosition(
            position => {
              setGeolocation({
                latitude: position.coords.latitude,
                longitude: position.coords.longitude,
              });
            },
            positionError => {
              setGeolocation(positionError.message);
            },
            { timeout: 5_000 }
          );
        }}
      >
        {'Get geolocation'}
      </button>

      <FormattedJSON value={geolocation} />
    </>
  );
}
