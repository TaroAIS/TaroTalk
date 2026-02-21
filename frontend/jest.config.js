const nextJest = require("next/jest");

const createJestConfig = nextJest({
  dir: "./"
});

const customJestConfig = {
  testEnvironment: "jsdom",
  setupFilesAfterEnv: ["<rootDir>/jest.setup.js"],
  moduleNameMapper: {
    "\\.(css|less|scss)$": "identity-obj-proxy"
  },
  transform: {
    "^.+\\.(ts|tsx)$": [
      "ts-jest",
      {
        tsconfig: {
          jsx: "react-jsx"
        }
      }
    ]
  }
};

module.exports = createJestConfig(customJestConfig);
