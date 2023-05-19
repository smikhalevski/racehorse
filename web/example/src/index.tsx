import { createRoot } from 'react-dom/client';
import { App } from './App';
import { createEventBridge } from 'racehorse';

const root = createRoot(document.body.appendChild(document.createElement('div')));

createEventBridge()
  .connect()
  .then(() => {
    root.render(<App />);
  });
