import nodeResolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import typescript from '@rollup/plugin-typescript';
import replace from '@rollup/plugin-replace';
import serve from 'rollup-plugin-serve';
import livereload from 'rollup-plugin-livereload';
import zip from 'rollup-plugin-zip';
import html from '@rollup/plugin-html';

export default {
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
      dir: './dist',
    }),
    process.env.ROLLUP_WATCH && serve('dist'),
    process.env.ROLLUP_WATCH && livereload('dist'),
  ],
};
