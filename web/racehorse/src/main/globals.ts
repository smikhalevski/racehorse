import { createEventBridge } from './createEventBridge';
import { createNetworkManager } from './createNetworkManager';
import { createActionsManager } from './createActionsManager';
import { createConfigurationManager } from './createConfigurationManager';
import { createGooglePlayReferrerManager } from './createGooglePlayReferrerManager';
import { createPermissionsManager } from './createPermissionsManager';
import { createFirebaseManager } from './createFirebaseManager';

/**
 * The default {@linkcode EventBridge}.
 */
export const eventBridge = createEventBridge();

/**
 * The default {@linkcode ActionsManager}.
 */
export const actionsManager = createActionsManager(eventBridge);

/**
 * The default {@linkcode ConfigurationManager}.
 */
export const configurationManager = createConfigurationManager(eventBridge);

/**
 * The default {@linkcode FirebaseManager}.
 */
export const firebaseManager = createFirebaseManager(eventBridge);

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
