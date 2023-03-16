import { createEventBridge } from './createEventBridge';
import { createNetworkManager } from './createNetworkManager';
import { createActionsManager } from './createActionsManager';
import { createDeviceManager } from './createDeviceManager';
import { createGooglePlayReferrerManager } from './createGooglePlayReferrerManager';
import { createPermissionsManager } from './createPermissionsManager';

/**
 * The default {@linkcode EventBridge}.
 */
export const eventBridge = createEventBridge();

/**
 * The default {@linkcode ActionsManager}.
 */
export const actionsManager = createActionsManager(eventBridge);

/**
 * The default {@linkcode DeviceManager}.
 */
export const deviceManager = createDeviceManager(eventBridge);

/**
 * The default {@linkcode GooglePlayReferrerManager}.
 */
export const googlePlayReferrerManager = createGooglePlayReferrerManager(eventBridge);

/**
 * The default {@linkcode NetworkManager}.
 */
export const networkManager = createNetworkManager(eventBridge);

/**
 * The default {@linkcode PermissionsManager}.
 */
export const permissionsManager = createPermissionsManager(eventBridge);
