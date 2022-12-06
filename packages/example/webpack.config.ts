import webpack from 'webpack';
import 'webpack-dev-server';
import path from 'path';

const configuration: webpack.Configuration = {
  entry: './src/main/index.ts',
  output: {
    path: path.resolve(__dirname, 'target'),
    filename: 'index.js',
  },
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        exclude: /node_modules/,
        loader: 'ts-loader',
      },
    ],
  },
  devServer: {
    static: path.resolve(__dirname, 'src/main/static'),
  },
};

export default configuration;
