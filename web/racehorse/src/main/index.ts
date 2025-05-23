import { createActivityManager } from './createActivityManager.js';
import { createBiometricEncryptedStorageManager } from './createBiometricEncryptedStorageManager.js';
import { createBiometricManager } from './createBiometricManager.js';
import { createContactsManager } from './createContactsManager.js';
import { createDeepLinkManager } from './createDeepLinkManager.js';
import { createDeviceManager } from './createDeviceManager.js';
import { createDownloadManager } from './createDownloadManager.js';
import { createEncryptedStorageManager } from './createEncryptedStorageManager.js';
import { createEventBridge } from './createEventBridge.js';
import { createEvergreenManager } from './createEvergreenManager.js';
import { createFacebookLoginManager } from './createFacebookLoginManager.js';
import { createFacebookShareManager } from './createFacebookShareManager.js';
import { createFirebaseManager } from './createFirebaseManager.js';
import { createFsManager } from './createFsManager.js';
import { createGooglePayManager } from './createGooglePayManager.js';
import { createGooglePlayReferrerManager } from './createGooglePlayReferrerManager.js';
import { createGoogleSignInManager } from './createGoogleSignInManager.js';
import { createKeyboardManager } from './createKeyboardManager.js';
import { createNetworkManager } from './createNetworkManager.js';
import { createNotificationsManager } from './createNotificationsManager.js';
import { createPermissionsManager } from './createPermissionsManager.js';

export { createActivityManager, Intent, Activity, ActivityState } from './createActivityManager.js';
export { createBiometricEncryptedStorageManager } from './createBiometricEncryptedStorageManager.js';
export { createBiometricManager, BiometricStatus, BiometricAuthenticator } from './createBiometricManager.js';
export { createContactsManager } from './createContactsManager.js';
export { createDeepLinkManager } from './createDeepLinkManager.js';
export { createDeviceManager, InsetType } from './createDeviceManager.js';
export { createDownloadManager, DownloadStatus, DownloadReason } from './createDownloadManager.js';
export { createEncryptedStorageManager } from './createEncryptedStorageManager.js';
export { createEventBridge } from './createEventBridge.js';
export { createEvergreenManager } from './createEvergreenManager.js';
export { createFacebookLoginManager } from './createFacebookLoginManager.js';
export { createFacebookShareManager } from './createFacebookShareManager.js';
export { createFirebaseManager } from './createFirebaseManager.js';
export { createFsManager, File, Directory } from './createFsManager.js';
export {
  createGooglePayManager,
  GooglePayTokenState,
  GooglePayTokenServiceProvider,
  GooglePayCardNetwork,
} from './createGooglePayManager.js';
export { createGooglePlayReferrerManager } from './createGooglePlayReferrerManager.js';
export { createGoogleSignInManager } from './createGoogleSignInManager.js';
export { createKeyboardManager } from './createKeyboardManager.js';
export { createNetworkManager } from './createNetworkManager.js';
export { createNotificationsManager } from './createNotificationsManager.js';
export { createPermissionsManager } from './createPermissionsManager.js';
export { createScheduler } from './createScheduler.js';
export { runAnimation } from './runAnimation.js';
export { scrollToElement } from './scrollToElement.js';

export type { ActivityManager, ActivityResult, ActivityInfo } from './createActivityManager.js';
export type { BiometricEncryptedStorageManager, BiometricConfig } from './createBiometricEncryptedStorageManager.js';
export type { BiometricManager } from './createBiometricManager.js';
export type { ContactsManager, Contact } from './createContactsManager.js';
export type { DeepLinkManager } from './createDeepLinkManager.js';
export type { DeviceManager, DeviceInfo, Rect } from './createDeviceManager.js';
export type { DownloadManager, DownloadOptions, Download } from './createDownloadManager.js';
export type { EncryptedStorageManager } from './createEncryptedStorageManager.js';
export type { EventBridge, Event, Connection } from './createEventBridge.js';
export type { EvergreenManager, UpdateStatus, BundleInfo, UpdateMode } from './createEvergreenManager.js';
export type { FacebookLoginManager, FacebookAccessToken } from './createFacebookLoginManager.js';
export type { FacebookShareManager, FacebookShareLinkContent } from './createFacebookShareManager.js';
export type { FirebaseManager } from './createFirebaseManager.js';
export type { FileAttributes, FsManager } from './createFsManager.js';
export type {
  GooglePayUserAddress,
  GooglePayTokenStatus,
  GooglePayTokenizeRequest,
  GooglePayPushTokenizeRequest,
  GooglePayTokenInfo,
  GooglePayManager,
} from './createGooglePayManager.js';
export type { GooglePlayReferrerManager } from './createGooglePlayReferrerManager.js';
export type { GoogleSignInManager, GoogleSignInAccount } from './createGoogleSignInManager.js';
export type { KeyboardManager } from './createKeyboardManager.js';
export type { NetworkManager, NetworkType, NetworkStatus } from './createNetworkManager.js';
export type { NotificationsManager } from './createNotificationsManager.js';
export type { PermissionsManager } from './createPermissionsManager.js';
export type { Scheduler } from './createScheduler.js';
export type { ScrollToElementOptions } from './scrollToElement.js';
export type { Animation, AnimationHandler, TweenAnimation, Easing, Unsubscribe } from './types.js';

/**
 * Event bridge delivers events from and to native Android.
 */
export const eventBridge = createEventBridge();

/**
 * Launches activities for various intents, and provides info about the current activity.
 */
export const activityManager = createActivityManager(eventBridge);

/**
 * A biometric encrypted key-value file-based storage.
 */
export const biometricEncryptedStorageManager = createBiometricEncryptedStorageManager(eventBridge);

/**
 * Provides the status of biometric support and allows to enroll for biometric auth.
 */
export const biometricManager = createBiometricManager(eventBridge);

/**
 * Provides access to phone contacts.
 */
export const contactsManager = createContactsManager(eventBridge);

/**
 * Monitors deep link requests.
 */
export const deepLinkManager = createDeepLinkManager(eventBridge);

/**
 * Device configuration and general device information.
 */
export const deviceManager = createDeviceManager(eventBridge);

/**
 * Allows starting and monitoring file downloads.
 */
export const downloadManager = createDownloadManager(eventBridge);

/**
 * Handles background updates.
 */
export const evergreenManager = createEvergreenManager(eventBridge);

/**
 * Manages Facebook Login integration.
 */
export const facebookLoginManager = createFacebookLoginManager(eventBridge);

/**
 * Manages Facebook content sharing.
 */
export const facebookShareManager = createFacebookShareManager(eventBridge);

/**
 * File-based storage where each entry is encrypted with its own password.
 */
export const encryptedStorageManager = createEncryptedStorageManager(eventBridge);

/**
 * Provides access to Firebase configuration.
 */
export const firebaseManager = createFirebaseManager(eventBridge);

/**
 * File system CRUD operations.
 */
export const fsManager = createFsManager(eventBridge);

/**
 * Manages tokenized cards in Google Pay.
 */
export const googlePayManager = createGooglePayManager(eventBridge);

/**
 * Gets [Google Play referrer](https://developer.android.com/google/play/installreferrer/library) information.
 */
export const googlePlayReferrerManager = createGooglePlayReferrerManager(eventBridge);

/**
 * Manages Google Sign-In integration.
 */
export const googleSignInManager = createGoogleSignInManager(eventBridge);

/**
 * Manages keyboard visibility and provides its status updates.
 */
export const keyboardManager = createKeyboardManager(eventBridge);

/**
 * Monitors the network status.
 */
export const networkManager = createNetworkManager(eventBridge);

/**
 * Manages system notifications.
 */
export const notificationsManager = createNotificationsManager(eventBridge);

/**
 * Checks permission statuses and ask for permissions.
 */
export const permissionsManager = createPermissionsManager(eventBridge);
