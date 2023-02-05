import { EventBridge } from './createEventBridge';

export interface PermissionManager {
  shouldShowRequestPermissionRationale(permission: string): Promise<boolean>;

  shouldShowRequestPermissionRationale(permissions: string[]): Promise<{ [permission: string]: boolean }>;

  isPermissionGranted(permission: string): Promise<boolean>;

  isPermissionGranted(permissions: string[]): Promise<{ [permission: string]: boolean }>;

  askForPermission(permission: string): Promise<boolean>;

  askForPermission(permissions: string[]): Promise<{ [permission: string]: boolean }>;
}

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
