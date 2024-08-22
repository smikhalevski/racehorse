import React, { useState } from 'react';
import { googleSignInManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';

export function GoogleSignInExample() {
  const [account, setAccount] = useState(googleSignInManager.getLastSignedInAccount);

  return (
    <>
      <h2>{'Google Sign-In'}</h2>
      {account !== null && (
        <p>
          <img
            style={{ display: 'block', width: '100px', height: '100px', borderRadius: '50%' }}
            src={account.photoUrl || ''}
            alt={account.displayName || ''}
          />
        </p>
      )}
      {'Account:'}
      <FormattedJSON value={account} />
      <p />
      <button
        className="btn btn-primary"
        onClick={() => {
          googleSignInManager.silentSignIn().then(setAccount);
        }}
      >
        {'Silent sign in'}
      </button>{' '}
      <button
        className="btn btn-primary"
        onClick={() => {
          googleSignInManager.signIn().then(setAccount);
        }}
      >
        {'Sign in'}
      </button>{' '}
      <button
        className="btn btn-primary"
        onClick={() => {
          googleSignInManager.signOut().then(() => {
            setAccount(null);
          });
        }}
      >
        {'Sign out'}
      </button>
    </>
  );
}
