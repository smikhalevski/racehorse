import React, { useState } from 'react';

export function GeolocationExample() {
  const [geolocation, setGeolocation] = useState<{ data?: { latitude: number; longitude: number }; error?: string }>();

  return (
    <>
      <h1>{'Geolocation'}</h1>

      <button
        className="btn btn-primary"
        onClick={() => {
          navigator.geolocation.getCurrentPosition(
            position => {
              setGeolocation({
                data: {
                  latitude: position.coords.latitude,
                  longitude: position.coords.longitude,
                },
              });
            },
            positionError => {
              setGeolocation({ error: positionError.message });
            }
          );
        }}
      >
        {'Get geolocation'}
      </button>

      {geolocation?.data !== undefined && (
        <div className="mt-3">
          <i className="bi-geo-fill text-danger me-2" />
          {'(' + geolocation.data.latitude + ', ' + geolocation.data.latitude + ')'}
        </div>
      )}
      {geolocation?.error !== undefined && <div className="mt-3">{geolocation.error}</div>}
    </>
  );
}
