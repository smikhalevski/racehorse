import nodeResolve from '@rollup/plugin-node-resolve';
import commonjs from '@rollup/plugin-commonjs';
import typescript from '@rollup/plugin-typescript';
import replace from '@rollup/plugin-replace';
import copy from 'rollup-plugin-copy';
import serve from 'rollup-plugin-serve';
import livereload from 'rollup-plugin-livereload';

export default {
  input: './src/index.tsx',
  output: { file: './dist/index.js', format: 'cjs' },
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
    copy({
      targets: [
        { src: './src/index.html', dest: 'dist' },
        { src: './src/style.css', dest: 'dist' },
      ],
    }),
    process.env.ROLLUP_WATCH && serve('dist'),
    process.env.ROLLUP_WATCH && livereload('dist'),
  ],
};
