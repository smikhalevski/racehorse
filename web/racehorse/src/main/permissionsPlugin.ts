import { EventBridge, Plugin } from './shared-types';

/**
 * Allows checking and requesting application permissions.
 */
export interface PermissionsMixin {
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
 * Check and ask for permissions.
 */
export const permissionsPlugin: Plugin<PermissionsMixin> = eventBridge => {
  eventBridge.shouldShowRequestPermissionRationale = permission => {
    return runOperationSync('org.racehorse.ShouldShowRequestPermissionRationaleRequestEvent', eventBridge, permission);
  };

  eventBridge.isPermissionGranted = permission => {
    return runOperationSync('org.racehorse.IsPermissionGrantedRequestEvent', eventBridge, permission);
  };

  eventBridge.askForPermission = permission => {
    return runOperation('org.racehorse.AskForPermissionRequestEvent', eventBridge, permission);
  };
};

function runOperation(type: string, eventBridge: EventBridge, permission: string | string[]): any {
  if (Array.isArray(permission)) {
    return eventBridge.request({ type, permissions: permission }).then(event => event.statuses || {});
  } else {
    return eventBridge.request({ type, permissions: [permission] }).then(event => event.statuses?.[permission]);
  }
}

function runOperationSync(type: string, eventBridge: EventBridge, permission: string | string[]): any {
  if (Array.isArray(permission)) {
    return eventBridge.requestSync({ type, permissions: permission })?.statuses || {};
  } else {
    return eventBridge.requestSync({ type, permissions: [permission] })?.statuses?.[permission];
  }
}