import React, { useEffect, useState } from 'react';
import { GoogleSignInAccount, googleSignInManager } from 'racehorse';
import { FormattedJSON } from '../components/FormattedJSON';

export function GoogleSignInExample() {
  const [account, setAccount] = useState<GoogleSignInAccount | null>();

  useEffect(() => {
    googleSignInManager.getLastSignedInAccount().then(setAccount);
  }, []);

  return (
    <>
      <h2>{'Google Sign-In'}</h2>
      <p>
        {'Account:'}
        <FormattedJSON value={account} />
      </p>
      <button
        onClick={() => {
          googleSignInManager.signIn().then(setAccount);
        }}
      >
        {'Sign in'}
      </button>{' '}
      <button
        onClick={() => {
          googleSignInManager.signOut().then(() => setAccount(null));
        }}
      >
        {'Sign out'}
      </button>
    </>
  );
}
