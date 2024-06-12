import React from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { eventBridge } from 'racehorse';
import 'abortcontroller-polyfill/dist/polyfill-patch-fetch';

if (process.env.NODE_ENV !== 'production') {
  new EventSource('http://10.0.2.2:10001/esbuild').addEventListener('change', () => {
    window.location.reload();
  });
}

eventBridge.connect().then(() => {
  createRoot(document.body.appendChild(document.createElement('div'))).render(<App />);
});
