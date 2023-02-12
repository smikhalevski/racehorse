import { createRoot } from 'react-dom/client';
import { App } from './App';
import { createEventBridge } from 'racehorse';

module.hot?.accept(() => {
  location.reload();
});

const root = createRoot(document.body.appendChild(document.createElement('div')));

createEventBridge()
  .waitForConnection()
  .then(() => {
    root.render(<App />);
  });
