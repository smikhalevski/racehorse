import { CookieExample } from './examples/CookieExample';
import { FileInputExample } from './examples/FileInputExample';
import { GeolocationExample } from './examples/GeolocationExample';
import { LocalStorageExample } from './examples/LocalStorageExample';
import { UseConfigurationExample } from './examples/UseConfigurationExample';
import { UseIntentsExample } from './examples/UseIntentsExample';
import { UseOnlineExample } from './examples/UseOnlineExample';
import { UsePermissionsExample } from './examples/UsePermissionsExample';

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
      <UseConfigurationExample />
      <UseIntentsExample />
      <UseOnlineExample />
      <UsePermissionsExample />
    </>
  );
}