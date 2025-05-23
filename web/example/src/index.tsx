import React from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App.js';
import { eventBridge } from 'racehorse';

eventBridge.connect().then(() => {
  createRoot(document.body.appendChild(document.createElement('div'))).render(<App />);
});
