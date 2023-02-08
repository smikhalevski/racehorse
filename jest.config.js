module.exports = {
  rootDir: process.cwd(),
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  modulePathIgnorePatterns: ['/lib/'],
  moduleNameMapper: {
    '^racehorse$': __dirname + '/web/racehorse/src/main',
    '^@racehorse/(.*)$': __dirname + '/web/$1/src/main',
  },
};
