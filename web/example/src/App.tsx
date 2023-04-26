import { CookieExample } from './examples/CookieExample';
import { FileInputExample } from './examples/FileInputExample';
import { GeolocationExample } from './examples/GeolocationExample';
import { LocalStorageExample } from './examples/LocalStorageExample';
import { DeviceExample } from './examples/DeviceExample';
import { OpenUrlExample } from './examples/OpenUrlExample';
import { NetworkExample } from './examples/NetworkExample';
import { NotificationsExample } from './examples/NotificationsExample';
import { PermissionsExample } from './examples/PermissionsExample';
import { EncryptedStorageExample } from './examples/EncryptedStorageExample';
import { ToastExample } from './examples/ToastExample';

module.hot?.accept(() => {
  location.reload();
});

export function App() {
  return (
    <>
      <ToastExample />
      <NotificationsExample />
      <EncryptedStorageExample />
      <CookieExample />
      <FileInputExample />
      <GeolocationExample />
      <LocalStorageExample />
      <DeviceExample />
      <OpenUrlExample />
      <NetworkExample />
      <PermissionsExample />
    </>
  );
}
