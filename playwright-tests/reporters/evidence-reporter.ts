import type { Reporter, TestCase, TestResult, TestStep } from '@playwright/test/reporter';
import fs from 'fs';
import path from 'path';

interface TestEvidence {
  testId: string;
  title: string;
  file: string;
  startTime: Date;
  endTime?: Date;
  duration?: number;
  status?: 'passed' | 'failed' | 'timedOut' | 'skipped';
  error?: string;
  steps: StepEvidence[];
  annotations: any[];
  attachments: AttachmentEvidence[];
  metadata: {
    project: string;
    browser?: string;
    retry?: number;
    workerIndex?: number;
  };
}

interface StepEvidence {
  title: string;
  category: string;
  startTime: Date;
  endTime?: Date;
  duration?: number;
  error?: string;
  location?: string;
}

interface AttachmentEvidence {
  name: string;
  contentType: string;
  path?: string;
  body?: Buffer;
}

class EvidenceReporter implements Reporter {
  private evidenceDir: string;
  private testEvidence: Map<string, TestEvidence> = new Map();
  private currentTestFile: string = '';

  constructor() {
    this.evidenceDir = path.join(__dirname, '../../test_probe/evidence');
    this.ensureDirectories();
  }

  private ensureDirectories() {
    const dirs = [
      this.evidenceDir,
      path.join(this.evidenceDir, 'test-runs'),
      path.join(this.evidenceDir, 'screenshots'),
      path.join(this.evidenceDir, 'videos'),
      path.join(this.evidenceDir, 'traces'),
      path.join(this.evidenceDir, 'logs'),
      path.join(this.evidenceDir, 'debate-transcripts'),
      path.join(this.evidenceDir, 'performance-metrics'),
      path.join(this.evidenceDir, 'database-snapshots')
    ];

    dirs.forEach(dir => {
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
      }
    });
  }

  onBegin(config: any, suite: any) {
    const timestamp = new Date().toISOString();
    console.log(`\n📊 Evidence Reporter: Test run started at ${timestamp}`);
    console.log(`📁 Evidence will be saved to: ${this.evidenceDir}`);
    
    // Create test run summary file
    const runInfo = {
      startTime: timestamp,
      config: {
        workers: config.workers,
        projects: config.projects.map((p: any) => p.name),
        timeout: config.timeout,
        retries: config.retries
      },
      totalTests: suite.allTests().length
    };
    
    fs.writeFileSync(
      path.join(this.evidenceDir, 'test-runs', `run-${timestamp.replace(/[:.]/g, '-')}.json`),
      JSON.stringify(runInfo, null, 2)
    );
  }

  onTestBegin(test: TestCase) {
    const testId = this.getTestId(test);
    this.currentTestFile = test.location.file;
    
    this.testEvidence.set(testId, {
      testId,
      title: test.title,
      file: test.location.file,
      startTime: new Date(),
      steps: [],
      annotations: test.annotations,
      attachments: [],
      metadata: {
        project: test.parent.project()?.name || 'unknown',
        retry: test.results.length
      }
    });

    console.log(`\n🧪 Starting test: ${test.title}`);
    console.log(`📍 Location: ${test.location.file}:${test.location.line}`);
  }

  onStepBegin(test: TestCase, result: TestResult, step: TestStep) {
    const testId = this.getTestId(test);
    const evidence = this.testEvidence.get(testId);
    
    if (evidence && step.category !== 'hook') {
      evidence.steps.push({
        title: step.title,
        category: step.category,
        startTime: new Date(),
        location: step.location?.file
      });
      
      // Log important steps
      if (step.category === 'test.step' || step.title.includes('LLM') || step.title.includes('debate')) {
        console.log(`  📌 ${step.title}`);
      }
    }
  }

  onStepEnd(test: TestCase, result: TestResult, step: TestStep) {
    const testId = this.getTestId(test);
    const evidence = this.testEvidence.get(testId);
    
    if (evidence) {
      const lastStep = evidence.steps[evidence.steps.length - 1];
      if (lastStep) {
        lastStep.endTime = new Date();
        lastStep.duration = lastStep.endTime.getTime() - lastStep.startTime.getTime();
        if (step.error) {
          lastStep.error = step.error.message;
        }
      }
    }
  }

  async onTestEnd(test: TestCase, result: TestResult) {
    const testId = this.getTestId(test);
    const evidence = this.testEvidence.get(testId);
    
    if (evidence) {
      evidence.endTime = new Date();
      evidence.duration = result.duration;
      evidence.status = result.status;
      
      if (result.error) {
        evidence.error = result.error.message + '\n' + result.error.stack;
      }
      
      // Process attachments
      for (const attachment of result.attachments) {
        await this.processAttachment(testId, attachment);
        evidence.attachments.push({
          name: attachment.name,
          contentType: attachment.contentType,
          path: attachment.path
        });
      }
      
      // Save test evidence
      await this.saveTestEvidence(evidence);
      
      // Log test result
      const statusEmoji = {
        passed: '✅',
        failed: '❌',
        timedOut: '⏱️',
        skipped: '⏭️'
      };
      
      console.log(`\n${statusEmoji[result.status] || '❓'} Test ${result.status}: ${test.title}`);
      console.log(`⏱️  Duration: ${result.duration}ms`);
      
      if (result.error) {
        console.log(`💥 Error: ${result.error.message}`);
      }
      
      // Special handling for debate tests
      if (test.title.includes('debate') || test.title.includes('LLM')) {
        await this.saveDebateTranscript(evidence);
      }
    }
  }

  private async processAttachment(testId: string, attachment: any) {
    if (!attachment.path && !attachment.body) return;
    
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    let targetPath: string;
    
    switch (attachment.contentType) {
      case 'image/png':
      case 'image/jpeg':
        targetPath = path.join(this.evidenceDir, 'screenshots', `${testId}-${timestamp}-${attachment.name}`);
        break;
      case 'video/webm':
      case 'video/mp4':
        targetPath = path.join(this.evidenceDir, 'videos', `${testId}-${timestamp}-${attachment.name}`);
        break;
      case 'application/zip':
        targetPath = path.join(this.evidenceDir, 'traces', `${testId}-${timestamp}-${attachment.name}`);
        break;
      default:
        targetPath = path.join(this.evidenceDir, 'logs', `${testId}-${timestamp}-${attachment.name}`);
    }
    
    if (attachment.path) {
      // Copy file to evidence directory
      fs.copyFileSync(attachment.path, targetPath);
    } else if (attachment.body) {
      // Write buffer to file
      fs.writeFileSync(targetPath, attachment.body);
    }
    
    attachment.path = targetPath;
  }

  private async saveTestEvidence(evidence: TestEvidence) {
    const timestamp = evidence.startTime.toISOString().replace(/[:.]/g, '-');
    const filename = `${evidence.metadata.project}-${timestamp}-${evidence.testId}.json`;
    const filepath = path.join(this.evidenceDir, 'test-runs', filename);
    
    fs.writeFileSync(filepath, JSON.stringify(evidence, null, 2));
  }

  private async saveDebateTranscript(evidence: TestEvidence) {
    // Extract debate-related information from test steps
    const debateSteps = evidence.steps.filter(step => 
      step.title.includes('debate') || 
      step.title.includes('turn') || 
      step.title.includes('participant')
    );
    
    if (debateSteps.length > 0) {
      const transcript = {
        testId: evidence.testId,
        testTitle: evidence.title,
        timestamp: evidence.startTime,
        duration: evidence.duration,
        debateInteractions: debateSteps,
        llmCalls: evidence.steps.filter(step => step.title.includes('LLM')),
        errors: evidence.steps.filter(step => step.error).map(step => ({
          step: step.title,
          error: step.error
        }))
      };
      
      const filename = `debate-${evidence.testId}-${evidence.startTime.toISOString().replace(/[:.]/g, '-')}.json`;
      fs.writeFileSync(
        path.join(this.evidenceDir, 'debate-transcripts', filename),
        JSON.stringify(transcript, null, 2)
      );
    }
  }

  async onEnd(result: any) {
    const endTime = new Date().toISOString();
    console.log(`\n📊 Evidence Reporter: Test run completed at ${endTime}`);
    
    // Generate summary report
    const summary = {
      endTime,
      duration: result.duration,
      status: result.status,
      stats: {
        total: 0,
        passed: 0,
        failed: 0,
        timedOut: 0,
        skipped: 0
      },
      failedTests: [] as any[],
      slowestTests: [] as any[]
    };
    
    // Collect statistics
    const allTests = Array.from(this.testEvidence.values());
    summary.stats.total = allTests.length;
    
    allTests.forEach(test => {
      if (test.status) {
        summary.stats[test.status]++;
        if (test.status === 'failed') {
          summary.failedTests.push({
            title: test.title,
            file: test.file,
            error: test.error,
            duration: test.duration
          });
        }
      }
    });
    
    // Find slowest tests
    summary.slowestTests = allTests
      .filter(t => t.duration)
      .sort((a, b) => (b.duration || 0) - (a.duration || 0))
      .slice(0, 10)
      .map(t => ({
        title: t.title,
        duration: t.duration,
        project: t.metadata.project
      }));
    
    // Save summary
    fs.writeFileSync(
      path.join(this.evidenceDir, 'test-summary.json'),
      JSON.stringify(summary, null, 2)
    );
    
    // Print summary
    console.log('\n📈 Test Summary:');
    console.log(`  Total: ${summary.stats.total}`);
    console.log(`  ✅ Passed: ${summary.stats.passed}`);
    console.log(`  ❌ Failed: ${summary.stats.failed}`);
    console.log(`  ⏱️  Timed out: ${summary.stats.timedOut}`);
    console.log(`  ⏭️  Skipped: ${summary.stats.skipped}`);
    
    if (summary.failedTests.length > 0) {
      console.log('\n❌ Failed Tests:');
      summary.failedTests.forEach(test => {
        console.log(`  - ${test.title}`);
        console.log(`    ${test.error?.split('\n')[0]}`);
      });
    }
    
    console.log(`\n📁 Evidence saved to: ${this.evidenceDir}`);
  }

  private getTestId(test: TestCase): string {
    return `${test.parent.project()?.name || 'default'}-${test.title.replace(/[^a-zA-Z0-9]/g, '-')}`;
  }
}

export default EvidenceReporter;