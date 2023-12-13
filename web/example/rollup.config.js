const nodeResolve = require('@rollup/plugin-node-resolve');
const commonjs = require('@rollup/plugin-commonjs');
const typescript = require('@rollup/plugin-typescript');
const replace = require('@rollup/plugin-replace');
const serve = require('rollup-plugin-serve');
const livereload = require('rollup-plugin-livereload');
const zip = require('rollup-plugin-zip');
const html = require('@rollup/plugin-html');

module.exports = {
  input: './src/index.tsx',
  output: { dir: './dist', format: 'iife' },
  plugins: [
    nodeResolve(),
    replace({
      values: {
        'process.env.NODE_ENV': JSON.stringify('production'),
      },
      preventAssignment: true,
    }),
    commonjs(),
    typescript({ tsconfig: './tsconfig.json' }),
    html({
      title: 'Racehorse',
    }),
    zip({
      file: 'bundle.zip',
    }),
    process.env.ROLLUP_WATCH && serve('dist'),
    process.env.ROLLUP_WATCH && livereload('dist'),
  ],
};
