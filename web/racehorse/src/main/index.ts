import { createActivityManager } from './createActivityManager';
import { createBiometricEncryptedStorageManager } from './createBiometricEncryptedStorageManager';
import { createBiometricManager } from './createBiometricManager';
import { createDeepLinkManager } from './createDeepLinkManager';
import { createDeviceManager } from './createDeviceManager';
import { createDownloadManager } from './createDownloadManager';
import { createEncryptedStorageManager } from './createEncryptedStorageManager';
import { createEventBridge } from './createEventBridge';
import { createEvergreenManager } from './createEvergreenManager';
import { createFacebookLoginManager } from './createFacebookLoginManager';
import { createFacebookShareManager } from './createFacebookShareManager';
import { createFirebaseManager } from './createFirebaseManager';
import { createGooglePayManager } from './createGooglePayManager';
import { createGooglePlayReferrerManager } from './createGooglePlayReferrerManager';
import { createGoogleSignInManager } from './createGoogleSignInManager';
import { createKeyboardManager } from './createKeyboardManager';
import { createNetworkManager } from './createNetworkManager';
import { createNotificationsManager } from './createNotificationsManager';
import { createPermissionsManager } from './createPermissionsManager';
import { createScheduler } from './createScheduler';

export { createActivityManager, Intent, Activity, ActivityState } from './createActivityManager';
export { createBiometricEncryptedStorageManager } from './createBiometricEncryptedStorageManager';
export { createBiometricManager, BiometricStatus, BiometricAuthenticator } from './createBiometricManager';
export { createDeepLinkManager } from './createDeepLinkManager';
export { createDeviceManager, InsetType } from './createDeviceManager';
export { createDownloadManager, DownloadStatus, DownloadReason } from './createDownloadManager';
export { createEncryptedStorageManager } from './createEncryptedStorageManager';
export { createEventBridge } from './createEventBridge';
export { createEvergreenManager } from './createEvergreenManager';
export { createFacebookLoginManager } from './createFacebookLoginManager';
export { createFacebookShareManager } from './createFacebookShareManager';
export { createFirebaseManager } from './createFirebaseManager';
export {
  createGooglePayManager,
  GooglePayTokenState,
  GooglePayTokenServiceProvider,
  GooglePayCardNetwork,
} from './createGooglePayManager';
export { createGooglePlayReferrerManager } from './createGooglePlayReferrerManager';
export { createGoogleSignInManager } from './createGoogleSignInManager';
export { createJoiner } from './createJoiner';
export { createKeyboardManager } from './createKeyboardManager';
export { createNetworkManager } from './createNetworkManager';
export { createNotificationsManager } from './createNotificationsManager';
export { createPermissionsManager } from './createPermissionsManager';
export { createScheduler } from './createScheduler';

export type { ActivityManager, ActivityResult, ActivityInfo } from './createActivityManager';
export type { BiometricEncryptedStorageManager, BiometricConfig } from './createBiometricEncryptedStorageManager';
export type { BiometricManager } from './createBiometricManager';
export type { DeepLinkManager } from './createDeepLinkManager';
export type { DeviceManager, DeviceInfo, Rect } from './createDeviceManager';
export type { DownloadManager, DownloadOptions, Download } from './createDownloadManager';
export type { EncryptedStorageManager } from './createEncryptedStorageManager';
export type { EventBridge, Event, Connection } from './createEventBridge';
export type { EvergreenManager, UpdateStatus, UpdateMode } from './createEvergreenManager';
export type { FacebookLoginManager, FacebookAccessToken } from './createFacebookLoginManager';
export type { FacebookShareManager, FacebookShareLinkContent } from './createFacebookShareManager';
export type { FirebaseManager } from './createFirebaseManager';
export type {
  GooglePayUserAddress,
  GooglePayTokenStatus,
  GooglePayTokenizeRequest,
  GooglePayPushTokenizeRequest,
  GooglePayTokenInfo,
  GooglePayManager,
} from './createGooglePayManager';
export type { GooglePlayReferrerManager } from './createGooglePlayReferrerManager';
export type { GoogleSignInManager, GoogleSignInAccount } from './createGoogleSignInManager';
export type { Joiner } from './createJoiner';
export type { KeyboardManager, KeyboardStatus } from './createKeyboardManager';
export type { NetworkManager, NetworkType, NetworkStatus } from './createNetworkManager';
export type { NotificationsManager } from './createNotificationsManager';
export type { PermissionsManager } from './createPermissionsManager';
export type { Scheduler } from './createScheduler';

export const uiScheduler = createScheduler();

export const eventBridge = createEventBridge();

export const activityManager = createActivityManager(eventBridge, uiScheduler);

export const biometricEncryptedStorageManager = createBiometricEncryptedStorageManager(eventBridge, uiScheduler);

export const biometricManager = createBiometricManager(eventBridge, uiScheduler);

export const deepLinkManager = createDeepLinkManager(eventBridge);

export const deviceManager = createDeviceManager(eventBridge);

export const downloadManager = createDownloadManager(eventBridge);

export const evergreenManager = createEvergreenManager(eventBridge);

export const facebookLoginManager = createFacebookLoginManager(eventBridge, uiScheduler);

export const facebookShareManager = createFacebookShareManager(eventBridge, uiScheduler);

export const encryptedStorageManager = createEncryptedStorageManager(eventBridge);

export const firebaseManager = createFirebaseManager(eventBridge);

export const googlePayManager = createGooglePayManager(eventBridge, uiScheduler);

export const googlePlayReferrerManager = createGooglePlayReferrerManager(eventBridge);

export const googleSignInManager = createGoogleSignInManager(eventBridge, uiScheduler);

export const keyboardManager = createKeyboardManager(eventBridge);

export const networkManager = createNetworkManager(eventBridge);

export const notificationsManager = createNotificationsManager(eventBridge);

export const permissionsManager = createPermissionsManager(eventBridge, uiScheduler);
