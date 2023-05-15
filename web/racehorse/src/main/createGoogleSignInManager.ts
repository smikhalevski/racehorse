import { EventBridge } from './types';
import { ensureEvent } from './utils';

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

  getLastSignedInAccount(): Promise<GoogleSignInAccount | null>;

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
  const getLastSignedInAccount = () =>
    eventBridge
      .request({ type: 'org.racehorse.auth.GetLastGoogleSignedInAccountEvent' })
      .then(event => ensureEvent(event).account);

  const signIn = () =>
    eventBridge.request({ type: 'org.racehorse.auth.GoogleSignInEvent' }).then(event => ensureEvent(event).account);

  return {
    getLastSignedInAccountOrSignIn: () =>
      getLastSignedInAccount().then(account => {
        return account?.isExpired === false ? account : signIn();
      }),

    getLastSignedInAccount,

    signIn,

    signOut: () =>
      eventBridge.request({ type: 'org.racehorse.auth.GoogleSignOutEvent' }).then(event => {
        ensureEvent(event);
      }),

    revokeAccess: () =>
      eventBridge.request({ type: 'org.racehorse.auth.GoogleRevokeAccessEvent' }).then(event => {
        ensureEvent(event);
      }),
  };
}
