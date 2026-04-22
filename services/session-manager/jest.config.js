module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'node',
  testMatch: ['**/__tests__/**/*.test.ts'],
  collectCoverageFrom: ['src/**/*.ts', '!src/server.ts', '!src/index.ts'],
  coverageReporters: ['text', 'json-summary', 'lcov'],
};
