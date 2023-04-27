import { createEventBridge } from './createEventBridge';
import { createActivityManager } from './createActivityManager';
import { createDeepLinkManager } from './createDeepLinkManager';
import { createDeviceManager } from './createDeviceManager';
import { createEncryptedStorageManager } from './createEncryptedStorageManager';
import { createEvergreenManager } from './createEvergreenManager';
import { createFirebaseManager } from './createFirebaseManager';
import { createGooglePlayReferrerManager } from './createGooglePlayReferrerManager';
import { createKeyboardManager } from './createKeyboardManager';
import { createNetworkManager } from './createNetworkManager';
import { createNotificationsManager } from './createNotificationsManager';
import { createPermissionsManager } from './createPermissionsManager';

export const eventBridge = createEventBridge();

export const activityManager = createActivityManager(eventBridge);

export const deepLinkManager = createDeepLinkManager(eventBridge);

export const deviceManager = createDeviceManager(eventBridge);

export const evergreenManager = createEvergreenManager(eventBridge);

export const encryptedStorageManager = createEncryptedStorageManager(eventBridge);

export const firebaseManager = createFirebaseManager(eventBridge);

export const googlePlayReferrerManager = createGooglePlayReferrerManager(eventBridge);

export const keyboardManager = createKeyboardManager(eventBridge);

export const networkManager = createNetworkManager(eventBridge);

export const notificationsManager = createNotificationsManager(eventBridge);

export const permissionsManager = createPermissionsManager(eventBridge);
