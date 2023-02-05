import { EventBridge } from './createEventBridge';

/**
 * Allows checking and requesting application permissions.
 */
export interface PermissionManager {
  /**
   * Gets whether you should show UI with rationale before requesting a permission.
   *
   * @param permission A permission your app wants to request.
   * @returns Whether you should show permission rationale UI.
   */
  shouldShowRequestPermissionRationale(permission: string): Promise<boolean>;

  /**
   * Gets whether you should show UI with rationale before requesting a permission.
   *
   * @param permissions An array of permission your app wants to request.
   * @returns Mapping from permission name to a boolean indicating whether you should show permission rationale UI.
   */
  shouldShowRequestPermissionRationale(permissions: string[]): Promise<{ [permission: string]: boolean }>;

  /**
   * Returns `true` if you have been granted a particular permission, or `false` otherwise.
   *
   * @param permission The name of the permission being checked.
   */
  isPermissionGranted(permission: string): Promise<boolean>;

  /**
   * Returns a mapping from a permission name to a boolean indicating that you have been granted a particular permission.
   *
   * @param permissions An array of permission your app wants to request.
   */
  isPermissionGranted(permissions: string[]): Promise<{ [permission: string]: boolean }>;

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
 * Creates the new {@linkcode PermissionManager} instance.
 *
 * @param eventBridge The event bridge to use for communication with Android device.
 */
export function createPermissionManager(eventBridge: EventBridge): PermissionManager {
  return {
    shouldShowRequestPermissionRationale(permissions: string | string[]) {
      return runOperation('org.racehorse.ShouldShowRequestPermissionRationaleRequestEvent', eventBridge, permissions);
    },

    isPermissionGranted(permissions: string | string[]) {
      return runOperation('org.racehorse.IsPermissionGrantedRequestEvent', eventBridge, permissions);
    },

    askForPermission(permissions: string | string[]) {
      return runOperation('org.racehorse.AskForPermissionRequestEvent', eventBridge, permissions);
    },
  };
}

function runOperation(type: string, eventBridge: EventBridge, permission: string | string[]): any {
  if (Array.isArray(permission)) {
    return eventBridge.request({ type, permissions: permission }).then(event => {
      return event.ok ? event.statuses : {};
    });
  } else {
    return eventBridge.request({ type, permissions: [permission] }).then(event => {
      return event.ok ? event.statuses[permission] : undefined;
    });
  }
}
