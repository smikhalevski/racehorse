import { defineConfig } from 'vite';
import zip from 'rollup-plugin-zip';

export default defineConfig(env => {
  return {
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
    esbuild: {
      target: 'ES2015',
    },
    resolve: {
      preserveSymlinks: true,
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
        transformIndexHtml: html => (env.command === 'serve' ? html : html.replaceAll('type="module"', 'defer')),
      },
      zip({
        file: 'bundle.zip',
      }),
    ],
  };
});
