import { createEventBridge } from './createEventBridge';
import { createActivityManager } from './createActivityManager';
import { createBiometricEncryptedStorageManager } from './createBiometricEncryptedStorageManager';
import { createBiometricManager } from './createBiometricManager';
import { createDeepLinkManager } from './createDeepLinkManager';
import { createDeviceManager } from './createDeviceManager';
import { createDownloadManager } from './createDownloadManager';
import { createEncryptedStorageManager } from './createEncryptedStorageManager';
import { createEvergreenManager } from './createEvergreenManager';
import { createFacebookLoginManager } from './createFacebookLoginManager';
import { createFacebookShareManager } from './createFacebookShareManager';
import { createFirebaseManager } from './createFirebaseManager';
import { createGooglePayManager } from './createGooglePayManager';
import { createGooglePlayReferrerManager } from './createGooglePlayReferrerManager';
import { createGoogleSignInManager } from './createGoogleSignInManager';
import { createKeyboardManager } from './createKeyboardManager';
import { createLifecycleManager } from './createLifecycleManager';
import { createNetworkManager } from './createNetworkManager';
import { createNotificationsManager } from './createNotificationsManager';
import { createPermissionsManager } from './createPermissionsManager';

export * from './createBiometricEncryptedStorageManager';
export * from './createBiometricManager';
export * from './createDeepLinkManager';
export * from './createDeviceManager';
export * from './createDownloadManager';
export * from './createEncryptedStorageManager';
export * from './createEventBridge';
export * from './createEvergreenManager';
export * from './createFacebookLoginManager';
export * from './createFacebookShareManager';
export * from './createFirebaseManager';
export * from './createGooglePayManager';
export * from './createGooglePlayReferrerManager';
export * from './createGoogleSignInManager';
export * from './createActivityManager';
export * from './createKeyboardManager';
export * from './createLifecycleManager';
export * from './createNetworkManager';
export * from './createNotificationsManager';
export * from './createPermissionsManager';

export const eventBridge = createEventBridge();

export const activityManager = createActivityManager(eventBridge);

export const biometricEncryptedStorageManager = createBiometricEncryptedStorageManager(eventBridge);

export const biometricManager = createBiometricManager(eventBridge);

export const deepLinkManager = createDeepLinkManager(eventBridge);

export const deviceManager = createDeviceManager(eventBridge);

export const downloadManager = createDownloadManager(eventBridge);

export const evergreenManager = createEvergreenManager(eventBridge);

export const facebookLoginManager = createFacebookLoginManager(eventBridge);

export const facebookShareManager = createFacebookShareManager(eventBridge);

export const encryptedStorageManager = createEncryptedStorageManager(eventBridge);

export const firebaseManager = createFirebaseManager(eventBridge);

export const googlePayManager = createGooglePayManager(eventBridge);

export const googlePlayReferrerManager = createGooglePlayReferrerManager(eventBridge);

export const googleSignInManager = createGoogleSignInManager(eventBridge);

export const keyboardManager = createKeyboardManager(eventBridge);

export const lifecycleManager = createLifecycleManager(eventBridge);

export const networkManager = createNetworkManager(eventBridge);

export const notificationsManager = createNotificationsManager(eventBridge);

export const permissionsManager = createPermissionsManager(eventBridge);
