module.exports = {
  getInput: jest.fn(),
  setOutput: jest.fn(),
  setFailed: jest.fn(),
  info: jest.fn(),
  warning: jest.fn(),
  error: jest.fn(),
  debug: jest.fn(),
  startGroup: jest.fn(),
  endGroup: jest.fn(),
  saveState: jest.fn(),
  getState: jest.fn(),
  exportVariable: jest.fn(),
  addPath: jest.fn(),
  setSecret: jest.fn()
};