import React, { useState } from 'react';

export function GeolocationExample() {
  const [geolocation, setGeolocation] = useState<any>();

  return (
    <>
      <h2>{'GeolocationExample'}</h2>

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

      <pre style={{ whiteSpace: 'pre-wrap' }}>{JSON.stringify(geolocation, null, 2)}</pre>
    </>
  );
}
