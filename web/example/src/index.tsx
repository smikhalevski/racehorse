import { createRoot } from 'react-dom/client';
import { App } from './App';

module.hot?.accept(() => {
  location.reload();
});

const root = createRoot(document.body.appendChild(document.createElement('div')));

root.render(<App />);
