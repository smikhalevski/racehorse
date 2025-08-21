import { defineConfig } from 'vite';
import archiver from 'archiver';
import * as fs from 'node:fs';

export default defineConfig({
  root: './src',
  build: {
    minify: false,
    assetsDir: '.',
    outDir: '../dist',
    emptyOutDir: false,
    rollupOptions: {
      cache: true,
    },
  },
  server: {
    port: 10001,
    host: '127.0.0.1',
    hmr: {
      host: '10.0.2.2',
      port: 10001,
      protocol: 'ws',
    },
  },
  publicDir: '../dist',
  plugins: [
    {
      name: 'zip',
      writeBundle() {
        archiver('zip').directory('dist', false).pipe(fs.createWriteStream('dist/bundle.zip'));
      },
    },
  ],
});
