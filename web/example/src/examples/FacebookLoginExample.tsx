import React, { useState } from 'react';
import { facebookLoginManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';

export function FacebookLoginExample() {
  const [accessToken, setAccessToken] = useState(facebookLoginManager.getCurrentAccessToken);

  return (
    <>
      <h1>{'Facebook Login'}</h1>
      {accessToken !== null && (
        <p>
          <img
            style={{ display: 'block', width: '100px', height: '100px', borderRadius: '50%' }}
            src={`https://graph.facebook.com/${accessToken.userId}/picture?width=200&height=200`}
            alt={accessToken.userId}
          />
        </p>
      )}
      {'Access token:'}
      <FormattedJSON value={accessToken} />
      <p />
      <button
        className="btn btn-primary"
        onClick={() => {
          facebookLoginManager.logIn().then(setAccessToken);
        }}
      >
        {'Log in'}
      </button>{' '}
      <button
        className="btn btn-primary"
        onClick={() => {
          facebookLoginManager.logOut();
          setAccessToken(null);
        }}
      >
        {'Log out'}
      </button>
    </>
  );
}
