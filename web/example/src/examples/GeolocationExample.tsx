import React, { useState } from 'react';
import { FormattedJSON } from '../components/FormattedJSON';

export function GeolocationExample() {
  const [geolocation, setGeolocation] = useState<any>();

  return (
    <>
      <h2>{'Geolocation'}</h2>

      {window.location.protocol !== 'https:' && (
        <p style={{ color: 'red' }}>{'Pick "release" build variant to enable geolocation'}</p>
      )}

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
            }
          );
        }}
      >
        {'Get geolocation'}
      </button>

      <FormattedJSON value={geolocation} />
    </>
  );
}
