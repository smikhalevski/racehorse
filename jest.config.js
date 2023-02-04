module.exports = {
  rootDir: process.cwd(),
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  modulePathIgnorePatterns: ['/lib/'],
  moduleNameMapper: {
    '^racehorse$': __dirname + '/packages/racehorse/src/main',
    '^@racehorse/(.*)$': __dirname + '/packages/$1/src/main',
  },
};