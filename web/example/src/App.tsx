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
import { BiometricExample } from './examples/BiometricExample';
import { BiometricEncryptedStorageExample } from './examples/BiometricEncryptedStorageExample';
import { AssetLoaderExample } from './examples/AssetLoaderExample';
import { ContactsExample } from './examples/ContactsExample';
import { FsExample } from './examples/FsExample';
import { EvergreenExample } from './examples/EvergreenExample';
import { useKeyboardAnimation, useKeyboardManager, useWindowInsets } from '@racehorse/react';
import { runAnimation, scrollToElement } from 'racehorse';

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
    <>
      <EvergreenExample />
      <FsExample />
      <ContactsExample />
      <FileInputExample />
      <ToastExample />
      <AssetLoaderExample />
      <BiometricExample />
      <BiometricEncryptedStorageExample />
      {/*<DownloadExample />*/}
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
