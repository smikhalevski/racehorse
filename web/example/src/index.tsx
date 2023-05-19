import { createRoot } from 'react-dom/client';
import { App } from './App';
import { eventBridge } from 'racehorse';

const root = createRoot(document.body.appendChild(document.createElement('div')));

eventBridge.connect().then(() => {
  root.render(<App />);
});
