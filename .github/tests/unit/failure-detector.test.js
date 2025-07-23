const { describe, it, expect, jest, beforeEach, afterEach } = require('@jest/globals');
const core = require('@actions/core');
const github = require('@actions/github');

// Mock the modules
jest.mock('@actions/core');
jest.mock('@actions/github');

describe('Failure Detector Action', () => {
  let originalEnv;
  
  beforeEach(() => {
    // Save original environment
    originalEnv = process.env;
    process.env = { ...originalEnv };
    
    // Reset mocks
    jest.clearAllMocks();
    
    // Mock GitHub context
    github.context = {
      repo: { owner: 'test-owner', repo: 'test-repo' },
      runId: 123456,
      runNumber: 42,
      sha: 'abc123def456',
      ref: 'refs/heads/main',
      actor: 'test-user',
      payload: {
        head_commit: {
          message: 'Test commit message',
          author: { name: 'Test Author' }
        }
      }
    };
  });
  
  afterEach(() => {
    // Restore environment
    process.env = originalEnv;
  });
  
  describe('Input Validation', () => {
    it('should require workflow-name input', async () => {
      core.getInput.mockImplementation((name) => {
        if (name === 'workflow-name') return '';
        return 'default-value';
      });
      
      // Import and run the action
      jest.isolateModules(() => {
        require('../../actions/failure-detector/index.js');
      });
      
      expect(core.setFailed).toHaveBeenCalledWith(
        expect.stringContaining('workflow-name')
      );
    });
    
    it('should use default values for optional inputs', async () => {
      core.getInput.mockImplementation((name) => {
        if (name === 'workflow-name') return 'Test Workflow';
        return '';
      });
      
      const mockOctokit = {
        rest: {
          actions: {
            getWorkflowRun: jest.fn().mockResolvedValue({
              data: {
                id: 123,
                html_url: 'https://github.com/test/test/actions/runs/123',
                conclusion: 'failure'
              }
            }),
            listJobsForWorkflowRun: jest.fn().mockResolvedValue({
              data: { jobs: [] }
            })
          }
        }
      };
      
      github.getOctokit.mockReturnValue(mockOctokit);
      
      jest.isolateModules(() => {
        require('../../actions/failure-detector/index.js');
      });
      
      // Wait for async operations
      await new Promise(resolve => setTimeout(resolve, 100));
      
      expect(core.setOutput).toHaveBeenCalledWith('should-create-issue', 'true');
    });
  });
  
  describe('Failure Detection', () => {
    it('should detect failed jobs correctly', async () => {
      core.getInput.mockImplementation((name) => {
        if (name === 'workflow-name') return 'Test Workflow';
        if (name === 'severity') return 'high';
        return '';
      });
      
      const mockOctokit = {
        rest: {
          actions: {
            getWorkflowRun: jest.fn().mockResolvedValue({
              data: {
                id: 123,
                html_url: 'https://github.com/test/test/actions/runs/123',
                conclusion: 'failure'
              }
            }),
            listJobsForWorkflowRun: jest.fn().mockResolvedValue({
              data: {
                jobs: [
                  {
                    id: 1,
                    name: 'Build',
                    conclusion: 'failure',
                    html_url: 'https://github.com/test/test/actions/runs/123/jobs/1',
                    steps: [
                      {
                        name: 'Compile',
                        conclusion: 'failure',
                        number: 3
                      }
                    ]
                  },
                  {
                    id: 2,
                    name: 'Test',
                    conclusion: 'success',
                    steps: []
                  }
                ]
              }
            })
          }
        }
      };
      
      github.getOctokit.mockReturnValue(mockOctokit);
      
      jest.isolateModules(() => {
        require('../../actions/failure-detector/index.js');
      });
      
      // Wait for async operations
      await new Promise(resolve => setTimeout(resolve, 100));
      
      const issueDataCall = core.setOutput.mock.calls.find(
        call => call[0] === 'issue-data'
      );
      
      expect(issueDataCall).toBeDefined();
      
      const issueData = JSON.parse(issueDataCall[1]);
      expect(issueData.failure.jobs).toHaveLength(1);
      expect(issueData.failure.jobs[0].name).toBe('Build');
      expect(issueData.failure.severity).toBe('high');
    });
    
    it('should categorize workflows correctly', async () => {
      const testCases = [
        { name: 'CI/CD Pipeline', expectedCategory: 'ci-cd' },
        { name: 'Security Scan', expectedCategory: 'security' },
        { name: 'Code Quality Check', expectedCategory: 'code-quality' },
        { name: 'Deploy to Production', expectedCategory: 'deployment' },
        { name: 'Unit Tests', expectedCategory: 'testing' },
        { name: 'Random Workflow', expectedCategory: 'general' }
      ];
      
      for (const testCase of testCases) {
        jest.clearAllMocks();
        
        core.getInput.mockImplementation((name) => {
          if (name === 'workflow-name') return testCase.name;
          return '';
        });
        
        const mockOctokit = {
          rest: {
            actions: {
              getWorkflowRun: jest.fn().mockResolvedValue({
                data: {
                  id: 123,
                  html_url: 'https://github.com/test/test/actions/runs/123',
                  conclusion: 'failure'
                }
              }),
              listJobsForWorkflowRun: jest.fn().mockResolvedValue({
                data: { jobs: [] }
              })
            }
          }
        };
        
        github.getOctokit.mockReturnValue(mockOctokit);
        
        jest.isolateModules(() => {
          require('../../actions/failure-detector/index.js');
        });
        
        await new Promise(resolve => setTimeout(resolve, 100));
        
        const issueDataCall = core.setOutput.mock.calls.find(
          call => call[0] === 'issue-data'
        );
        
        const issueData = JSON.parse(issueDataCall[1]);
        expect(issueData.failure.category).toBe(testCase.expectedCategory);
      }
    });
  });
  
  describe('Error Pattern Detection', () => {
    it('should detect error patterns from step names', async () => {
      core.getInput.mockImplementation((name) => {
        if (name === 'workflow-name') return 'Test Workflow';
        return '';
      });
      
      const mockOctokit = {
        rest: {
          actions: {
            getWorkflowRun: jest.fn().mockResolvedValue({
              data: {
                id: 123,
                html_url: 'https://github.com/test/test/actions/runs/123',
                conclusion: 'failure'
              }
            }),
            listJobsForWorkflowRun: jest.fn().mockResolvedValue({
              data: {
                jobs: [
                  {
                    id: 1,
                    name: 'CI',
                    conclusion: 'failure',
                    steps: [
                      { name: 'Run Tests', conclusion: 'failure', number: 1 },
                      { name: 'ESLint Check', conclusion: 'failure', number: 2 },
                      { name: 'Build Application', conclusion: 'failure', number: 3 },
                      { name: 'Security Scan', conclusion: 'failure', number: 4 }
                    ]
                  }
                ]
              }
            })
          }
        }
      };
      
      github.getOctokit.mockReturnValue(mockOctokit);
      
      jest.isolateModules(() => {
        require('../../actions/failure-detector/index.js');
      });
      
      await new Promise(resolve => setTimeout(resolve, 100));
      
      const issueDataCall = core.setOutput.mock.calls.find(
        call => call[0] === 'issue-data'
      );
      
      const issueData = JSON.parse(issueDataCall[1]);
      expect(issueData.failure.errorPatterns).toContain('test-failure');
      expect(issueData.failure.errorPatterns).toContain('linting-failure');
      expect(issueData.failure.errorPatterns).toContain('build-failure');
      expect(issueData.failure.errorPatterns).toContain('security-failure');
    });
  });
  
  describe('Pull Request Context', () => {
    it('should include PR information when available', async () => {
      github.context.payload.pull_request = {
        number: 123,
        title: 'Test PR',
        user: { login: 'test-user' }
      };
      
      core.getInput.mockImplementation((name) => {
        if (name === 'workflow-name') return 'Test Workflow';
        return '';
      });
      
      const mockOctokit = {
        rest: {
          actions: {
            getWorkflowRun: jest.fn().mockResolvedValue({
              data: {
                id: 123,
                html_url: 'https://github.com/test/test/actions/runs/123',
                conclusion: 'failure'
              }
            }),
            listJobsForWorkflowRun: jest.fn().mockResolvedValue({
              data: { jobs: [] }
            })
          }
        }
      };
      
      github.getOctokit.mockReturnValue(mockOctokit);
      
      jest.isolateModules(() => {
        require('../../actions/failure-detector/index.js');
      });
      
      await new Promise(resolve => setTimeout(resolve, 100));
      
      const issueDataCall = core.setOutput.mock.calls.find(
        call => call[0] === 'issue-data'
      );
      
      const issueData = JSON.parse(issueDataCall[1]);
      expect(issueData.context.pullRequest).toBeDefined();
      expect(issueData.context.pullRequest.number).toBe(123);
      expect(issueData.context.pullRequest.title).toBe('Test PR');
      expect(issueData.context.pullRequest.author).toBe('test-user');
    });
  });
});