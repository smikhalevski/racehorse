import { createRoot } from 'react-dom/client';
import { App } from './App';
import { eventBridge } from 'racehorse';

eventBridge.connect().then(() => {
  createRoot(document.body.appendChild(document.createElement('div'))).render(<App />);
});
