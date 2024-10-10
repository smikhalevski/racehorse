import React from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { eventBridge } from 'racehorse';
import 'abortcontroller-polyfill/dist/polyfill-patch-fetch';
import 'bootstrap/dist/css/bootstrap.css';
import 'bootstrap/dist/js/bootstrap.js';
import 'bootstrap-icons/font/bootstrap-icons.css';

eventBridge.connect().then(() => {
  createRoot(document.body.appendChild(document.createElement('div'))).render(<App />);
});
