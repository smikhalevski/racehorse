import { CookieExample } from './examples/CookieExample';
import { FileInputExample } from './examples/FileInputExample';
import { GeolocationExample } from './examples/GeolocationExample';
import { LocalStorageExample } from './examples/LocalStorageExample';
import { DeviceManagerExample } from './examples/DeviceManagerExample';
import { ActionsManagerExample } from './examples/ActionsManagerExample';
import { UseOnlineExample } from './examples/UseOnlineExample';
import { PermissionsManagerExample } from './examples/PermissionsManagerExample';

module.hot?.accept(() => {
  location.reload();
});

export function App() {
  return (
    <>
      <CookieExample />
      <FileInputExample />
      <GeolocationExample />
      <LocalStorageExample />
      <DeviceManagerExample />
      <ActionsManagerExample />
      <UseOnlineExample />
      <PermissionsManagerExample />
    </>
  );
}
