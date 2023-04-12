import { CookieExample } from './examples/CookieExample';
import { FileInputExample } from './examples/FileInputExample';
import { GeolocationExample } from './examples/GeolocationExample';
import { LocalStorageExample } from './examples/LocalStorageExample';
import { ConfigurationManagerExample } from './examples/ConfigurationManagerExample';
import { ActionsManagerExample } from './examples/ActionsManagerExample';
import { NetworkExample } from './examples/NetworkExample';
import { PermissionsManagerExample } from './examples/PermissionsManagerExample';
import { EncryptedKeyValueStorageManagerExample } from './examples/EncryptedKeyValueStorageManagerExample';
import { ToastExample } from './examples/ToastExample';

module.hot?.accept(() => {
  location.reload();
});

export function App() {
  return (
    <>
      <ToastExample />
      <EncryptedKeyValueStorageManagerExample />
      <CookieExample />
      <FileInputExample />
      <GeolocationExample />
      <LocalStorageExample />
      <ConfigurationManagerExample />
      <ActionsManagerExample />
      <NetworkExample />
      <PermissionsManagerExample />
    </>
  );
}
