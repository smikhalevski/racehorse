import React, { useLayoutEffect } from 'react';
import { CookieExample } from './examples/CookieExample';
import { FileInputExample } from './examples/FileInputExample';
import { GeolocationExample } from './examples/GeolocationExample';
import { KeyboardExample } from './examples/KeyboardExample';
import { GoogleSignInExample } from './examples/GoogleSignInExample';
import { LocalStorageExample } from './examples/LocalStorageExample';
import { DeviceExample } from './examples/DeviceExample';
import { ActivityExample } from './examples/ActivityExample';
import { NetworkExample } from './examples/NetworkExample';
import { NotificationsExample } from './examples/NotificationsExample';
import { PermissionsExample } from './examples/PermissionsExample';
import { EncryptedStorageExample } from './examples/EncryptedStorageExample';
import { ToastExample } from './examples/ToastExample';
import { EventBridgeExample } from './examples/EventBridgeExample';
import { FacebookLoginExample } from './examples/FacebookLoginExample';
import { FacebookShareExample } from './examples/FacebookShareExample';
import { DownloadExample } from './examples/DownloadExample';
import { BiometricExample } from './examples/BiometricExample';
import { BiometricEncryptedStorageExample } from './examples/BiometricEncryptedStorageExample';
import { AssetLoaderExample } from './examples/AssetLoaderExample';
import { ContactsExample } from './examples/ContactsExample';
import { FsExample } from './examples/FsExample';
import { EvergreenExample } from './examples/EvergreenExample';
import { useKeyboardAnimationHandler, useWindowInsets } from '@racehorse/react';

export function App() {
  const windowInsets = useWindowInsets();

  useLayoutEffect(() => {
    document.body.style.padding =
      windowInsets.top + 'px ' + windowInsets.right + 'px ' + windowInsets.bottom + 'px ' + windowInsets.left + 'px';
  }, [windowInsets]);

  useKeyboardAnimationHandler((_animation, height, _percent) => {
    document.body.style.paddingBottom = Math.max(height, windowInsets.bottom) + 'px';

    if (document.activeElement !== null && document.activeElement !== document.body) {
      document.activeElement.scrollIntoView({ block: 'center' });
    }
  });

  return (
    <>
      <EvergreenExample />
      <FsExample />
      <ContactsExample />
      <FileInputExample />
      <ToastExample />
      <AssetLoaderExample />
      <BiometricExample />
      <BiometricEncryptedStorageExample />
      <DownloadExample />
      <KeyboardExample />
      <FacebookShareExample />
      <GoogleSignInExample />
      <FacebookLoginExample />
      <PermissionsExample />
      <ActivityExample />
      <NotificationsExample />
      <NetworkExample />
      <EncryptedStorageExample />
      <CookieExample />
      <GeolocationExample />
      <LocalStorageExample />
      <DeviceExample />
      <EventBridgeExample />
    </>
  );
}
