const typescript = require('@rollup/plugin-typescript');
const path = require('path');

const pkg = require(path.resolve('package.json'));

const external = Object.keys(Object.assign({}, pkg.dependencies, pkg.peerDependencies));

module.exports = [
  {
    input: './lib/index.ts',
    output: [
      { format: 'cjs', entryFileNames: '[name].js', dir: './lib', preserveModules: true, sourcemap: 'inline' },
      { format: 'es', entryFileNames: '[name].mjs', dir: './lib', preserveModules: true, sourcemap: 'inline' },
    ],
    external,
    plugins: [typescript({ tsconfig: './tsconfig.build.json' })],
  },
];
