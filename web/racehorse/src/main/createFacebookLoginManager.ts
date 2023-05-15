import { EventBridge } from './types';
import { ensureEvent } from './utils';

export interface FacebookAccessToken {
  expires: string;
  permissions: string[];
  declinedPermissions: string[];
  expiredPermissions: string[];
  token: string;
  source: string;
  lastRefresh: string;
  applicationId: string;
  userId: string;
  dataAccessExpirationTime: string;
  graphDomain: string;
  isExpired: boolean;
  isDataAccessExpired: boolean;
  isInstagramToken: boolean;
}

export interface FacebookLoginManager {
  getCurrentAccessTokenOrLogIn(permissions?: string[]): Promise<FacebookAccessToken | null>;

  getCurrentAccessToken(): Promise<FacebookAccessToken | null>;

  logIn(permissions?: string[]): Promise<FacebookAccessToken | null>;

  logOut(): Promise<void>;
}

/**
 * Manages Facebook Login integration.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createFacebookLoginManager(eventBridge: EventBridge): FacebookLoginManager {
  const getCurrentAccessToken = () =>
    eventBridge
      .request({ type: 'org.racehorse.auth.GetCurrentFacebookAccessTokenEvent' })
      .then(event => ensureEvent(event).accessToken);

  const logIn = (permissions?: string[]) =>
    eventBridge
      .request({ type: 'org.racehorse.auth.FacebookLogInEvent', permissions })
      .then(event => ensureEvent(event).accessToken);

  return {
    getCurrentAccessTokenOrLogIn: permissions =>
      getCurrentAccessToken().then(accessToken => {
        return accessToken?.isExpired === false ? accessToken : logIn(permissions);
      }),

    getCurrentAccessToken,

    logIn,

    logOut: () =>
      eventBridge.request({ type: 'org.racehorse.auth.FacebookLogOutEvent' }).then(event => {
        ensureEvent(event);
      }),
  };
}
