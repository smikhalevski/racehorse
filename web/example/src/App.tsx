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
      <nav className="navbar sticky-top bg-body-tertiary shadow">
        <div
          className="container-fluid"
          style={{ margin: windowInsets.top + 'px ' + windowInsets.right + 'px 0 ' + windowInsets.left + 'px' }}
        >
          <div className="navbar-brand">{'Download'}</div>
          <button
            className="navbar-toggler"
            type="button"
            data-bs-toggle="offcanvas"
            data-bs-target="#offcanvasNavbar"
            aria-controls="offcanvasNavbar"
            aria-label="Toggle navigation"
          >
            <span className="navbar-toggler-icon" />
          </button>
          <div
            className="offcanvas offcanvas-end"
            tabIndex={-1}
            id="offcanvasNavbar"
            aria-labelledby="offcanvasNavbarLabel"
            style={{ padding: windowInsets.top + 'px ' + windowInsets.right + 'px ' + windowInsets.bottom + 'px 0' }}
          >
            <div className="offcanvas-header">
              <h4
                className="offcanvas-title"
                id="offcanvasNavbarLabel"
              >
                {'Racehorse'}
              </h4>
              <button
                type="button"
                className="btn-close"
                data-bs-dismiss="offcanvas"
                aria-label="Close"
              />
            </div>
            <div className="offcanvas-body">
              <ul className="navbar-nav">
                <li className="nav-item">
                  <a
                    className="nav-link active"
                    aria-current="page"
                    href="#"
                  >
                    Home
                  </a>
                </li>
                <li className="nav-item">
                  <a
                    className="nav-link"
                    aria-current="page"
                    href="#"
                  >
                    Home2
                  </a>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </nav>
      <main
        style={{ padding: '0 ' + windowInsets.right + 'px ' + windowInsets.bottom + 'px ' + windowInsets.left + 'px' }}
      >
        <div className="p-3">
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
        </div>
      </main>
    </>
  );
}
