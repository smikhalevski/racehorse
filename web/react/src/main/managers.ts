import {
  ActivityManager,
  activityManager,
  BiometricEncryptedStorageManager,
  biometricEncryptedStorageManager,
  BiometricManager,
  biometricManager,
  ContactsManager,
  contactsManager,
  DeepLinkManager,
  deepLinkManager,
  DeviceManager,
  deviceManager,
  DownloadManager,
  downloadManager,
  EncryptedStorageManager,
  encryptedStorageManager,
  EventBridge,
  eventBridge,
  EvergreenManager,
  evergreenManager,
  FacebookLoginManager,
  facebookLoginManager,
  FacebookShareManager,
  facebookShareManager,
  FirebaseManager,
  firebaseManager,
  FsManager,
  fsManager,
  GooglePayManager,
  googlePayManager,
  GooglePlayReferrerManager,
  googlePlayReferrerManager,
  GoogleSignInManager,
  googleSignInManager,
  KeyboardManager,
  keyboardManager,
  NetworkManager,
  networkManager,
  NotificationsManager,
  notificationsManager,
  PermissionsManager,
  permissionsManager,
} from 'racehorse';
import { createContext, useContext } from 'react';

const EventBridgeContext = createContext(eventBridge);

EventBridgeContext.displayName = 'EventBridgeContext';

export const EventBridgeProvider = EventBridgeContext.Provider;

export function useEventBridge(): EventBridge {
  return useContext(EventBridgeContext);
}

const ActivityManagerContext = createContext(activityManager);

ActivityManagerContext.displayName = 'ActivityManagerContext';

export const ActivityManagerProvider = ActivityManagerContext.Provider;

export function useActivityManager(): ActivityManager {
  return useContext(ActivityManagerContext);
}

const BiometricEncryptedStorageManagerContext = createContext(biometricEncryptedStorageManager);

BiometricEncryptedStorageManagerContext.displayName = 'BiometricEncryptedStorageManagerContext';

export const BiometricEncryptedStorageManagerProvider = BiometricEncryptedStorageManagerContext.Provider;

export function useBiometricEncryptedStorageManager(): BiometricEncryptedStorageManager {
  return useContext(BiometricEncryptedStorageManagerContext);
}

const BiometricManagerContext = createContext(biometricManager);

BiometricManagerContext.displayName = 'BiometricManagerContext';

export const BiometricManagerProvider = BiometricManagerContext.Provider;

export function useBiometricManager(): BiometricManager {
  return useContext(BiometricManagerContext);
}

const ContactsManagerContext = createContext(contactsManager);

ContactsManagerContext.displayName = 'ContactsManagerContext';

export const ContactsManagerProvider = ContactsManagerContext.Provider;

export function useContactsManager(): ContactsManager {
  return useContext(ContactsManagerContext);
}

const DeepLinkManagerContext = createContext(deepLinkManager);

DeepLinkManagerContext.displayName = 'DeepLinkManagerContext';

export const DeepLinkManagerProvider = DeepLinkManagerContext.Provider;

export function useDeepLinkManager(): DeepLinkManager {
  return useContext(DeepLinkManagerContext);
}

const DeviceManagerContext = createContext(deviceManager);

DeviceManagerContext.displayName = 'DeviceManagerContext';

export const DeviceManagerProvider = DeviceManagerContext.Provider;

export function useDeviceManager(): DeviceManager {
  return useContext(DeviceManagerContext);
}

const DownloadManagerContext = createContext(downloadManager);

DownloadManagerContext.displayName = 'DownloadManagerContext';

export const DownloadManagerProvider = DownloadManagerContext.Provider;

export function useDownloadManager(): DownloadManager {
  return useContext(DownloadManagerContext);
}

const EvergreenManagerContext = createContext(evergreenManager);

EvergreenManagerContext.displayName = 'EvergreenManagerContext';

export const EvergreenManagerProvider = EvergreenManagerContext.Provider;

export function useEvergreenManager(): EvergreenManager {
  return useContext(EvergreenManagerContext);
}

const FacebookLoginManagerContext = createContext(facebookLoginManager);

FacebookLoginManagerContext.displayName = 'FacebookLoginManagerContext';

export const FacebookLoginManagerProvider = FacebookLoginManagerContext.Provider;

export function useFacebookLoginManager(): FacebookLoginManager {
  return useContext(FacebookLoginManagerContext);
}

const FacebookShareManagerContext = createContext(facebookShareManager);

FacebookShareManagerContext.displayName = 'FacebookShareManagerContext';

export const FacebookShareManagerProvider = FacebookShareManagerContext.Provider;

export function useFacebookShareManager(): FacebookShareManager {
  return useContext(FacebookShareManagerContext);
}

const EncryptedStorageManagerContext = createContext(encryptedStorageManager);

EncryptedStorageManagerContext.displayName = 'EncryptedStorageManagerContext';

export const EncryptedStorageManagerProvider = EncryptedStorageManagerContext.Provider;

export function useEncryptedStorageManager(): EncryptedStorageManager {
  return useContext(EncryptedStorageManagerContext);
}

const FirebaseManagerContext = createContext(firebaseManager);

FirebaseManagerContext.displayName = 'FirebaseManagerContext';

export const FirebaseManagerProvider = FirebaseManagerContext.Provider;

export function useFirebaseManager(): FirebaseManager {
  return useContext(FirebaseManagerContext);
}

const FsManagerContext = createContext(fsManager);

FsManagerContext.displayName = 'FsManagerContext';

export const FsManagerProvider = FsManagerContext.Provider;

export function useFsManager(): FsManager {
  return useContext(FsManagerContext);
}

const GooglePayManagerContext = createContext(googlePayManager);

GooglePayManagerContext.displayName = 'GooglePayManagerContext';

export const GooglePayManagerProvider = GooglePayManagerContext.Provider;

export function useGooglePayManager(): GooglePayManager {
  return useContext(GooglePayManagerContext);
}

const GooglePlayReferrerManagerContext = createContext(googlePlayReferrerManager);

GooglePlayReferrerManagerContext.displayName = 'GooglePlayReferrerManagerContext';

export const GooglePlayReferrerManagerProvider = GooglePlayReferrerManagerContext.Provider;

export function useGooglePlayReferrerManager(): GooglePlayReferrerManager {
  return useContext(GooglePlayReferrerManagerContext);
}

const GoogleSignInManagerContext = createContext(googleSignInManager);

GoogleSignInManagerContext.displayName = 'GoogleSignInManagerContext';

export const GoogleSignInManagerProvider = GoogleSignInManagerContext.Provider;

export function useGoogleSignInManager(): GoogleSignInManager {
  return useContext(GoogleSignInManagerContext);
}

const KeyboardManagerContext = createContext(keyboardManager);

KeyboardManagerContext.displayName = 'KeyboardManagerContext';

export const KeyboardManagerProvider = KeyboardManagerContext.Provider;

export function useKeyboardManager(): KeyboardManager {
  return useContext(KeyboardManagerContext);
}

const NetworkManagerContext = createContext(networkManager);

NetworkManagerContext.displayName = 'NetworkManagerContext';

export const NetworkManagerProvider = NetworkManagerContext.Provider;

export function useNetworkManager(): NetworkManager {
  return useContext(NetworkManagerContext);
}

const NotificationsManagerContext = createContext(notificationsManager);

NotificationsManagerContext.displayName = 'NotificationsManagerContext';

export const NotificationsManagerProvider = NotificationsManagerContext.Provider;

export function useNotificationsManager(): NotificationsManager {
  return useContext(NotificationsManagerContext);
}

const PermissionsManagerContext = createContext(permissionsManager);

PermissionsManagerContext.displayName = 'PermissionsManagerContext';

export const PermissionsManagerProvider = PermissionsManagerContext.Provider;

export function usePermissionsManager(): PermissionsManager {
  return useContext(PermissionsManagerContext);
}
