import React, { useLayoutEffect } from 'react';
import { useKeyboardAnimation, useKeyboardManager, useWindowInsets } from '@racehorse/react';
import { runAnimation, scrollToElement } from 'racehorse';
import { LocalStorageExample } from './examples/LocalStorageExample';
import { KeyboardExample } from './examples/KeyboardExample';
import { ActivityExample } from './examples/ActivityExample';
import { AssetLoaderExample } from './examples/AssetLoaderExample';
import { BiometricEncryptedStorageExample } from './examples/BiometricEncryptedStorageExample';
import { BiometricExample } from './examples/BiometricExample';
import { ContactsExample } from './examples/ContactsExample';
import { CookieExample } from './examples/CookieExample';
import { DeviceExample } from './examples/DeviceExample';
import { DownloadExample } from './examples/DownloadExample';
import { EncryptedStorageExample } from './examples/EncryptedStorageExample';
import { EventBridgeExample } from './examples/EventBridgeExample';
import { EvergreenExample } from './examples/EvergreenExample';
import { FacebookLoginExample } from './examples/FacebookLoginExample';
import { FacebookShareExample } from './examples/FacebookShareExample';
import { FileInputExample } from './examples/FileInputExample';
import { FsExample } from './examples/FsExample';
import { GeolocationExample } from './examples/GeolocationExample';
import { GoogleSignInExample } from './examples/GoogleSignInExample';
import { NetworkExample } from './examples/NetworkExample';
import { NotificationsExample } from './examples/NotificationsExample';
import { PermissionsExample } from './examples/PermissionsExample';
import { ToastExample } from './examples/ToastExample';

export function App() {
  const keyboardManager = useKeyboardManager();
  const windowInsets = useWindowInsets();

  useLayoutEffect(() => {
    document.body.style.padding =
      windowInsets.top +
      'px ' +
      windowInsets.right +
      'px ' +
      Math.max(keyboardManager.getKeyboardHeight(), windowInsets.bottom) +
      'px ' +
      windowInsets.left +
      'px';
  }, [windowInsets]);

  useKeyboardAnimation((animation, signal) => {
    // Scroll to the active element when keyboard is shown
    if (
      animation.endValue !== 0 &&
      document.activeElement !== null &&
      document.activeElement !== document.body &&
      document.hasFocus()
    ) {
      scrollToElement(document.activeElement, {
        // Scroll animation has the same duration and easing as the keyboard animation
        animation,
        paddingBottom: animation.endValue,
        signal,
      });
    }

    runAnimation(
      animation,
      (animation, fraction) => {
        const keyboardHeight = animation.startValue + (animation.endValue - animation.startValue) * fraction;

        // Additional padding to compensate the keyboard height
        document.body.style.paddingBottom = Math.max(windowInsets.bottom, keyboardHeight) + 'px';
      },
      signal
    );
  });

  return (
    <main className="py-3 px-4">
      <ActivityExample />
      <AssetLoaderExample />
      <BiometricEncryptedStorageExample />
      <BiometricExample />
      <ContactsExample />
      <CookieExample />
      <DeviceExample />
      <DownloadExample />
      <EncryptedStorageExample />
      <EventBridgeExample />
      <EvergreenExample />
      <FacebookLoginExample />
      <FacebookShareExample />
      <FileInputExample />
      <FsExample />
      <GeolocationExample />
      <GoogleSignInExample />
      <KeyboardExample />
      <LocalStorageExample />
      <NetworkExample />
      <NotificationsExample />
      <PermissionsExample />
      <ToastExample />
    </main>
  );
}
