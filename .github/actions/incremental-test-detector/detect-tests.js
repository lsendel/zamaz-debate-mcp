#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const yaml = require('js-yaml');

class IncrementalTestDetector {
  constructor(options) {
    this.changedFiles = this.loadChangedFiles(options.changedFiles);
    this.mapping = this.loadMapping(options.mapping);
    this.includeDeps = options.includeDeps === 'true';
    this.minThreshold = parseInt(options.minThreshold);
    
    this.affectedServices = new Set();
    this.testsToRun = new Set();
    this.dependencyGraph = {};
  }

  loadChangedFiles(filePath) {
    const content = fs.readFileSync(filePath, 'utf8');
    return content.split('\n').filter(f => f.trim());
  }

  loadMapping(mappingPath) {
    if (!fs.existsSync(mappingPath)) {
      console.warn(`⚠️ Test mapping not found: ${mappingPath}`);
      return this.generateDefaultMapping();
    }
    
    const content = fs.readFileSync(mappingPath, 'utf8');
    return yaml.load(content);
  }

  generateDefaultMapping() {
    // Default mapping based on common project structure
    return {
      mappings: [
        {
          pattern: 'mcp-gateway/**/*.java',
          tests: ['mcp-gateway:unit', 'mcp-gateway:integration'],
          impacts: ['e2e:api']
        },
        {
          pattern: 'mcp-organization/**/*.java',
          tests: ['mcp-organization:unit', 'mcp-organization:integration'],
          impacts: ['e2e:api']
        },
        {
          pattern: 'mcp-controller/**/*.java',
          tests: ['mcp-controller:unit', 'mcp-controller:integration'],
          impacts: ['e2e:api', 'e2e:workflow']
        },
        {
          pattern: 'debate-ui/src/**/*',
          tests: ['debate-ui:unit', 'debate-ui:integration'],
          impacts: ['e2e:ui']
        },
        {
          pattern: '**/*.sql',
          tests: ['*:integration'],
          impacts: ['e2e:*']
        },
        {
          pattern: '**/pom.xml',
          tests: ['*:unit', '*:integration'],
          impacts: []
        },
        {
          pattern: '**/package.json',
          tests: ['*:unit', '*:integration'],
          impacts: []
        }
      ],
      dependencies: {
        'mcp-gateway': ['mcp-common'],
        'mcp-organization': ['mcp-common'],
        'mcp-controller': ['mcp-common', 'mcp-gateway'],
        'debate-ui': []
      }
    };
  }

  detect() {
    console.log(`🔍 Analyzing ${this.changedFiles.length} changed files...`);
    
    // Step 1: Direct mapping
    this.detectDirectMapping();
    
    // Step 2: Dependency analysis
    if (this.includeDeps) {
      this.detectDependencies();
    }
    
    // Step 3: Apply minimum threshold
    this.applyMinimumThreshold();
    
    // Step 4: Generate test matrix
    const matrix = this.generateMatrix();
    
    return {
      matrix,
      services: Array.from(this.affectedServices),
      testCount: this.testsToRun.size,
      skip: this.testsToRun.size === 0
    };
  }

  detectDirectMapping() {
    for (const file of this.changedFiles) {
      for (const mapping of this.mapping.mappings) {
        if (this.matchesPattern(file, mapping.pattern)) {
          // Add direct tests
          mapping.tests.forEach(test => {
            this.addTest(test, file);
          });
          
          // Add impact tests
          mapping.impacts.forEach(test => {
            this.addTest(test, file);
          });
        }
      }
    }
  }

  detectDependencies() {
    const dependencies = this.mapping.dependencies || {};
    const servicesToCheck = new Set(this.affectedServices);
    
    // Traverse dependency graph
    const checked = new Set();
    while (servicesToCheck.size > 0) {
      const service = servicesToCheck.values().next().value;
      servicesToCheck.delete(service);
      
      if (checked.has(service)) continue;
      checked.add(service);
      
      // Find services that depend on this one
      for (const [depService, deps] of Object.entries(dependencies)) {
        if (deps.includes(service) && !this.affectedServices.has(depService)) {
          this.affectedServices.add(depService);
          servicesToCheck.add(depService);
          
          // Add tests for dependent service
          this.addTest(`${depService}:unit`, `dependency:${service}`);
          this.addTest(`${depService}:integration`, `dependency:${service}`);
        }
      }
    }
  }

  applyMinimumThreshold() {
    // Count total available tests
    const allServices = ['mcp-gateway', 'mcp-organization', 'mcp-controller', 'debate-ui'];
    const allTestTypes = ['unit', 'integration'];
    const totalTests = allServices.length * allTestTypes.length;
    
    const currentPercentage = (this.testsToRun.size / totalTests) * 100;
    
    if (currentPercentage < this.minThreshold) {
      console.log(`⚠️ Only ${currentPercentage.toFixed(1)}% of tests selected, below ${this.minThreshold}% threshold`);
      console.log(`Adding core tests to meet minimum threshold...`);
      
      // Add core tests
      ['mcp-gateway:unit', 'mcp-controller:unit', 'debate-ui:unit'].forEach(test => {
        this.addTest(test, 'minimum-threshold');
      });
    }
  }

  matchesPattern(file, pattern) {
    // Convert glob pattern to regex
    const regex = pattern
      .replace(/\*\*/g, '.*')
      .replace(/\*/g, '[^/]*')
      .replace(/\?/g, '.');
    
    return new RegExp(`^${regex}$`).test(file);
  }

  addTest(testSpec, reason) {
    this.testsToRun.add(testSpec);
    
    // Extract service name
    const service = testSpec.split(':')[0];
    if (service !== '*') {
      this.affectedServices.add(service);
    }
    
    // Track why test was added
    if (!this.dependencyGraph[testSpec]) {
      this.dependencyGraph[testSpec] = [];
    }
    this.dependencyGraph[testSpec].push(reason);
  }

  generateMatrix() {
    const matrix = {
      include: []
    };
    
    // Expand wildcards and generate matrix entries
    for (const testSpec of this.testsToRun) {
      const [servicePattern, testType] = testSpec.split(':');
      
      if (servicePattern === '*') {
        // Run test type for all services
        ['mcp-gateway', 'mcp-organization', 'mcp-controller', 'debate-ui'].forEach(service => {
          if (testType === '*') {
            ['unit', 'integration'].forEach(type => {
              matrix.include.push({ service, suite: type });
            });
          } else {
            matrix.include.push({ service, suite: testType });
          }
        });
      } else {
        if (testType === '*') {
          ['unit', 'integration'].forEach(type => {
            matrix.include.push({ service: servicePattern, suite: type });
          });
        } else {
          matrix.include.push({ service: servicePattern, suite: testType });
        }
      }
    }
    
    // Remove duplicates
    const seen = new Set();
    matrix.include = matrix.include.filter(item => {
      const key = `${item.service}:${item.suite}`;
      if (seen.has(key)) return false;
      seen.add(key);
      return true;
    });
    
    return matrix;
  }

  saveResults(results) {
    // Save test matrix
    fs.writeFileSync('test-matrix.json', JSON.stringify(results.matrix, null, 2));
    
    // Save affected services
    fs.writeFileSync('affected-services.json', JSON.stringify(results.services, null, 2));
    
    // Save dependency graph
    fs.writeFileSync('dependency-graph.json', JSON.stringify(this.dependencyGraph, null, 2));
    
    // Output for GitHub Actions
    console.log(`::set-output name=matrix::${JSON.stringify(results.matrix)}`);
    console.log(`::set-output name=count::${results.testCount}`);
    console.log(`::set-output name=skip::${results.skip}`);
    console.log(`::set-output name=services::${results.services.join(',')}`);
  }
}

// Parse command line arguments
const args = process.argv.slice(2);
const options = {
  changedFiles: '',
  mapping: '.github/test-mapping.yml',
  includeDeps: 'true',
  minThreshold: '10'
};

for (let i = 0; i < args.length; i += 2) {
  const key = args[i].replace('--', '').replace('-', '');
  if (key in options) {
    options[key] = args[i + 1];
  }
}

// Run detection
const detector = new IncrementalTestDetector(options);
const results = detector.detect();
detector.saveResults(results);

console.log(`\n✅ Detection complete: ${results.testCount} test suites to run`);