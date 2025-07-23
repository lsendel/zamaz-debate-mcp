module.exports = {
  context: {
    repo: { owner: 'test-owner', repo: 'test-repo' },
    runId: 123456,
    runNumber: 42,
    sha: 'abc123def456',
    ref: 'refs/heads/main',
    actor: 'test-user',
    payload: {}
  },
  getOctokit: jest.fn()
};