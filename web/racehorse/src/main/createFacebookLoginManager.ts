import { EventBridge } from './createEventBridge';

export interface FacebookAccessToken {
  /**
   * The date at which the access token expires.
   */
  expires: number;

  /**
   * The list of permissions associated with this access token. Note that the most up-to-date list of permissions is
   * maintained by Facebook, so this list may be outdated if permissions have been added or removed since the time the
   * {@link FacebookAccessToken} object was created. For more information on permissions, see
   * [Permissions Reference](https://developers.facebook.com/docs/reference/login/#permissions).
   */
  permissions: string[];

  /**
   * The list of permissions declined by the user with this access token. It represents the entire set of permissions
   * that have been requested and declined. Note that the most up-to-date list of permissions is maintained by Facebook,
   * so this list may be outdated if permissions have been granted or declined since the last time an AccessToken object
   * was created.
   */
  declinedPermissions: string[];

  /**
   * The list of permissions that were expired with this access token.
   */
  expiredPermissions: string[];

  /**
   * The string representing the access token.
   */
  token: string;

  /**
   * The date at which the token was last refreshed. Since tokens expire, the Facebook SDK will attempt to renew them
   * periodically.
   */
  lastRefresh: number;

  /**
   * The ID of the Facebook Application associated with this access token.
   */
  applicationId: string;

  /**
   * The user id for this access token.
   */
  userId: string;

  /**
   * The date at which user data access expires.
   */
  dataAccessExpirationTime: string;

  /**
   * The graph domain for this access token.
   */
  graphDomain: 'facebook' | 'instagram' | string;

  /**
   * `true` if the token is expired.
   */
  isExpired: boolean;

  /**
   * `true` if the user data access is expired.
   */
  isDataAccessExpired: boolean;

  /**
   * `true` if the token is an Instagram access token, based on the token's {@link graphDomain} parameter.
   */
  isInstagramToken: boolean;
}

export interface FacebookLoginManager {
  /**
   * Getter for the access token that is current for the application.
   */
  getCurrentAccessToken(): FacebookAccessToken | null;

  /**
   * Logs the user in with the requested read permissions.
   *
   * @param permissions The requested permissions.
   */
  logIn(permissions?: string[]): Promise<FacebookAccessToken | null>;

  /**
   * Logs out the user.
   */
  logOut(): void;
}

/**
 * Manages Facebook Login integration.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createFacebookLoginManager(eventBridge: EventBridge): FacebookLoginManager {
  return {
    getCurrentAccessToken: () =>
      eventBridge.request({ type: 'org.racehorse.GetCurrentFacebookAccessTokenEvent' }).payload.accessToken,

    logIn: permissions =>
      eventBridge
        .requestAsync({ type: 'org.racehorse.FacebookLogInEvent', payload: { permissions } })
        .then(event => event.payload.accessToken),

    logOut() {
      eventBridge.request({ type: 'org.racehorse.FacebookLogOutEvent' });
    },
  };
}
