import { EventBridge } from './createEventBridge';

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

  getCurrentAccessToken(): FacebookAccessToken | null;

  logIn(permissions?: string[]): Promise<FacebookAccessToken | null>;

  logOut(): void;
}

/**
 * Manages Facebook Login integration.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createFacebookLoginManager(eventBridge: EventBridge): FacebookLoginManager {
  const getCurrentAccessToken = (): FacebookAccessToken =>
    eventBridge.request({ type: 'org.racehorse.auth.GetCurrentFacebookAccessTokenEvent' }).payload.accessToken;

  const logIn = (permissions?: string[]): Promise<FacebookAccessToken> =>
    eventBridge
      .requestAsync({ type: 'org.racehorse.auth.FacebookLogInEvent', payload: { permissions } })
      .then(event => event.payload.accessToken);

  return {
    getCurrentAccessTokenOrLogIn(permissions) {
      const accessToken = getCurrentAccessToken();

      return accessToken?.isExpired === false ? Promise.resolve(accessToken) : logIn(permissions);
    },

    getCurrentAccessToken,

    logIn,

    logOut() {
      eventBridge.request({ type: 'org.racehorse.auth.FacebookLogOutEvent' });
    },
  };
}
