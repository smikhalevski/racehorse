import { EventBridge } from './types';
import { ensureEvent } from './utils';

/**
 * Allows checking and requesting application permissions.
 */
export interface PermissionsManager {
  /**
   * Gets whether you should show UI with rationale before requesting a permission.
   *
   * @param permission A permission your app wants to request.
   * @returns Whether you should show permission rationale UI.
   */
  shouldShowRequestPermissionRationale(permission: string): boolean;

  /**
   * Gets whether you should show UI with rationale before requesting a permission.
   *
   * @param permissions An array of permission your app wants to request.
   * @returns Mapping from permission name to a boolean indicating whether you should show permission rationale UI.
   */
  shouldShowRequestPermissionRationale(permissions: string[]): { [permission: string]: boolean };

  /**
   * Returns `true` if you have been granted a particular permission, or `false` otherwise.
   *
   * @param permission The name of the permission being checked.
   */
  isPermissionGranted(permission: string): boolean;

  /**
   * Returns a mapping from a permission name to a boolean indicating that you have been granted a particular permission.
   *
   * @param permissions An array of permission your app wants to request.
   */
  isPermissionGranted(permissions: string[]): { [permission: string]: boolean };

  /**
   * Requests a permission to be granted to this application. This permission must be requested in your manifest, and
   * should not be granted to your app, and should have protection level dangerous, regardless whether it is declared by
   * the platform or a third-party app.
   *
   * @param permission The requested permission.
   */
  askForPermission(permission: string): Promise<boolean>;

  /**
   * Requests permissions to be granted to this application. These permissions must be requested in your manifest, they
   * should not be granted to your app, and they should have protection level dangerous, regardless whether they are
   * declared by the platform or a third-party app.
   *
   * @param permissions The requested permission.
   */
  askForPermission(permissions: string[]): Promise<{ [permission: string]: boolean }>;
}

/**
 * Check permission statuses and ask for permissions.
 *
 * @param eventBridge The underlying event bridge.
 */
export function createPermissionsManager(eventBridge: EventBridge): PermissionsManager {
  return {
    shouldShowRequestPermissionRationale: permission =>
      requestSync('org.racehorse.ShouldShowRequestPermissionRationaleEvent', eventBridge, permission),

    isPermissionGranted: permission => requestSync('org.racehorse.IsPermissionGrantedEvent', eventBridge, permission),

    askForPermission: permission => request('org.racehorse.AskForPermissionEvent', eventBridge, permission),
  };
}

function request(type: string, eventBridge: EventBridge, permission: string | string[]): any {
  if (Array.isArray(permission)) {
    return eventBridge
      .request({ type, payload: { permissions: permission } })
      .then(event => ensureEvent(event).payload.statuses);
  }

  return eventBridge
    .request({ type, payload: { permissions: [permission] } })
    .then(event => ensureEvent(event).payload.statuses[permission]);
}

function requestSync(type: string, eventBridge: EventBridge, permission: string | string[]): any {
  if (Array.isArray(permission)) {
    return ensureEvent(eventBridge.requestSync({ type, payload: { permissions: permission } })).payload.statuses;
  }

  return ensureEvent(eventBridge.requestSync({ type, payload: { permissions: [permission] } })).payload.statuses[
    permission
  ];
}
