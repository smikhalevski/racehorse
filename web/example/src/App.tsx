import React, { useLayoutEffect } from 'react';
import { CookieExample } from './examples/CookieExample.js';
import { FileInputExample } from './examples/FileInputExample.js';
import { GeolocationExample } from './examples/GeolocationExample.js';
import { KeyboardExample } from './examples/KeyboardExample.js';
import { GoogleSignInExample } from './examples/GoogleSignInExample.js';
import { LocalStorageExample } from './examples/LocalStorageExample.js';
import { DeviceExample } from './examples/DeviceExample.js';
import { ActivityExample } from './examples/ActivityExample.js';
import { NetworkExample } from './examples/NetworkExample.js';
import { NotificationsExample } from './examples/NotificationsExample.js';
import { PermissionsExample } from './examples/PermissionsExample.js';
import { EncryptedStorageExample } from './examples/EncryptedStorageExample.js';
import { ToastExample } from './examples/ToastExample.js';
import { EventBridgeExample } from './examples/EventBridgeExample.js';
import { FacebookLoginExample } from './examples/FacebookLoginExample.js';
import { FacebookShareExample } from './examples/FacebookShareExample.js';
import { BiometricExample } from './examples/BiometricExample.js';
import { BiometricEncryptedStorageExample } from './examples/BiometricEncryptedStorageExample.js';
import { AssetLoaderExample } from './examples/AssetLoaderExample.js';
import { ContactsExample } from './examples/ContactsExample.js';
import { FsExample } from './examples/FsExample.js';
import { EvergreenExample } from './examples/EvergreenExample.js';
import { useKeyboardAnimation, useKeyboardManager, useWindowInsets } from '@racehorse/react';
import { runAnimation, scrollToElement } from 'racehorse';
import { DownloadExample } from './examples/DownloadExample.js';
import { ProcessExample } from './examples/ProcessExample.js';

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
      <ProcessExample />
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
