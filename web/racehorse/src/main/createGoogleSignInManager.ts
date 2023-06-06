import { EventBridge } from './createEventBridge';
import { noop } from './utils';

export interface GoogleSignInAccount {
  id: string;
  idToken: string;
  email: string;
  grantedScopes: string[];
  serverAuthCode: string;
  isExpired: boolean;
  displayName: string;
  givenName: string;
  familyName: string;
  photoUrl: string;
}

export interface GoogleSignInManager {
  /**
   * Gets the last account that the user signed in with.
   */
  getLastSignedInAccount(): GoogleSignInAccount | null;

  /**
   * Starts the sign-in flow.
   *
   * The returned promise is rejected if an error occurs during sign in.
   *
   * @returns The account of the signed-in user or `null` if user cancelled the sign in.
   */
  signIn(): Promise<GoogleSignInAccount | null>;

  /**
   * Returns the account information for the user who is signed in to this app. If no user is signed in, try to sign
   * the user in without displaying any user interface.
   *
   * The account will possibly contain an ID token which may be used to authenticate and identify sessions that you
   * establish with your application servers. If you use the ID token expiry time to determine your session lifetime,
   * you should retrieve a refreshed ID token, by calling `silentSignIn` prior to each API call to your application
   * server.
   *
   * Calling {@link silentSignIn} can also help you detect user revocation of access to your application on other
   * platforms, and you can call {@link signIn} again to ask the user to re-authorize.
   *
   * If your user has never previously signed in to your app on the current device, we can still try to sign them in,
   * without displaying user interface, if they have signed in on a different device.
   *
   * We attempt to sign users in if there is one and only one matching account on the device that has previously signed
   * in to your application, and if the user previously granted all the scopes your app is requesting for this sign in.
   *
   * @returns The account of the signed-in user or `null` if user cannot be signed in is silently.
   */
  silentSignIn(): Promise<GoogleSignInAccount | null>;

  /**
   * Signs out the current signed-in user if any. It also clears the account previously selected by the user and a
   * future sign in attempt will require the user pick an account again.
   */
  signOut(): Promise<void>;

  /**
   * Revokes access given to the current application. Future sign-in attempts will require the user to re-consent to all
   * requested scopes. Applications are required to provide users that are signed in with Google the ability to
   * disconnect their Google account from the app. If the user deletes their account, you must delete the information
   * that your app obtained from the Google APIs.
   */
  revokeAccess(): Promise<void>;
}

/**
 * Manages Google Sign-In integration.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createGoogleSignInManager(eventBridge: EventBridge): GoogleSignInManager {
  return {
    getLastSignedInAccount: () =>
      eventBridge.request({ type: 'org.racehorse.auth.GetLastGoogleSignedInAccountEvent' }).payload.account,

    signIn: () =>
      eventBridge.requestAsync({ type: 'org.racehorse.auth.GoogleSignInEvent' }).then(event => event.payload.account),

    silentSignIn: () =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.auth.GoogleSilentSignInEvent' })
        .then(event => event.payload.account),

    signOut: () => eventBridge.requestAsync({ type: 'org.racehorse.auth.GoogleSignOutEvent' }).then(noop),

    revokeAccess: () => eventBridge.requestAsync({ type: 'org.racehorse.auth.GoogleRevokeAccessEvent' }).then(noop),
  };
}
