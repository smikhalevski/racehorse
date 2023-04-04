import { createEventBridge } from './createEventBridge';
import { createNetworkManager } from './createNetworkManager';
import { createActionsManager } from './createActionsManager';
import { createConfigurationManager } from './createConfigurationManager';
import { createGooglePlayReferrerManager } from './createGooglePlayReferrerManager';
import { createPermissionsManager } from './createPermissionsManager';
import { createFirebaseManager } from './createFirebaseManager';
import { createEncryptedKeyValueStorageManager } from './createEncryptedKeyValueStorageManager';

/**
 * The default {@link EventBridge}.
 */
export const eventBridge = createEventBridge();

/**
 * The default {@link ActionsManager}.
 */
export const actionsManager = createActionsManager(eventBridge);

/**
 * The default {@link ConfigurationManager}.
 */
export const configurationManager = createConfigurationManager(eventBridge);

/**
 * The default {@link EncryptedKeyValueStorageManager}.
 */
export const encryptedKeyValueStorageManager = createEncryptedKeyValueStorageManager(eventBridge);

/**
 * The default {@link FirebaseManager}.
 */
export const firebaseManager = createFirebaseManager(eventBridge);

/**
 * The default {@link GooglePlayReferrerManager}.
 */
export const googlePlayReferrerManager = createGooglePlayReferrerManager(eventBridge);

/**
 * The default {@link NetworkManager}.
 */
export const networkManager = createNetworkManager(eventBridge);

/**
 * The default {@link PermissionsManager}.
 */
export const permissionsManager = createPermissionsManager(eventBridge);
