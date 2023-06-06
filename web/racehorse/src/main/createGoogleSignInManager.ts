import { EventBridge } from './createEventBridge';
import { noop } from './utils';

export interface GoogleSignInAccount {
  id: string;
  idToken: string;
  email: string;
  displayName: string;
  photoUrl: string;
  serverAuthCode: string;
  expirationTimeSecs: string;
  obfuscatedIdentifier: string;
  givenName: string;
  familyName: string;
}

export interface GoogleSignInManager {
  getLastSignedInAccountOrSignIn(): Promise<GoogleSignInAccount | null>;

  getLastSignedInAccount(): GoogleSignInAccount | null;

  signIn(): Promise<GoogleSignInAccount | null>;

  signOut(): Promise<void>;

  revokeAccess(): Promise<void>;
}

/**
 * Manages Google Sign-In integration.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createGoogleSignInManager(eventBridge: EventBridge): GoogleSignInManager {
  const getLastSignedInAccount = (): GoogleSignInAccount =>
    eventBridge.request({ type: 'org.racehorse.auth.GetLastGoogleSignedInAccountEvent' }).payload.account;

  const signIn = () =>
    eventBridge.requestAsync({ type: 'org.racehorse.auth.GoogleSignInEvent' }).then(event => event.payload.account);

  return {
    getLastSignedInAccountOrSignIn() {
      return signIn();
    },

    getLastSignedInAccount,

    signIn,

    signOut: () => eventBridge.requestAsync({ type: 'org.racehorse.auth.GoogleSignOutEvent' }).then(noop),

    revokeAccess: () => eventBridge.requestAsync({ type: 'org.racehorse.auth.GoogleRevokeAccessEvent' }).then(noop),
  };
}
