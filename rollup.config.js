const typescript = require('@rollup/plugin-typescript');
const path = require('path');

const pkg = require(path.resolve('package.json'));

const external = Object.keys(Object.assign({}, pkg.dependencies, pkg.peerDependencies));

module.exports = [
  {
    input: './src/main/index.ts',
    output: [
      { format: 'cjs', entryFileNames: '[name].js', dir: './lib', preserveModules: true },
      { format: 'es', entryFileNames: '[name].mjs', dir: './lib', preserveModules: true },
    ],
    external,
    plugins: [typescript({ tsconfig: './tsconfig.build.json' })],
  },
];
