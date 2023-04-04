import { CookieExample } from './examples/CookieExample';
import { FileInputExample } from './examples/FileInputExample';
import { GeolocationExample } from './examples/GeolocationExample';
import { LocalStorageExample } from './examples/LocalStorageExample';
import { ConfigurationManagerExample } from './examples/ConfigurationManagerExample';
import { ActionsManagerExample } from './examples/ActionsManagerExample';
import { UseOnlineExample } from './examples/UseOnlineExample';
import { PermissionsManagerExample } from './examples/PermissionsManagerExample';
import { EncryptedKeyValueStorageManagerExample } from './examples/EncryptedKeyValueStorageManagerExample';

module.hot?.accept(() => {
  location.reload();
});

export function App() {
  return (
    <>
      <EncryptedKeyValueStorageManagerExample />
      <CookieExample />
      <FileInputExample />
      <GeolocationExample />
      <LocalStorageExample />
      <ConfigurationManagerExample />
      <ActionsManagerExample />
      <UseOnlineExample />
      <PermissionsManagerExample />
    </>
  );
}
