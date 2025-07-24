#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { DOMParser, XMLSerializer } = require('xmldom');

class TestResultMerger {
  constructor(workspace, output) {
    this.workspace = workspace;
    this.output = output;
    this.resultsDir = path.join(workspace, 'test-results');
  }

  merge() {
    console.log('📋 Merging test results...');
    
    // Find all result files
    const resultFiles = fs.readdirSync(this.resultsDir)
      .filter(f => f.endsWith('-results.xml'))
      .map(f => path.join(this.resultsDir, f));
    
    console.log(`Found ${resultFiles.length} result files to merge`);

    if (resultFiles.length === 0) {
      console.warn('⚠️ No test results found to merge');
      this.createEmptyResults();
      return;
    }

    // Parse and merge results
    const mergedDoc = this.createMergedDocument(resultFiles);
    
    // Write merged results
    const serializer = new XMLSerializer();
    const xmlString = serializer.serializeToString(mergedDoc);
    fs.writeFileSync(this.output, xmlString);
    
    console.log(`✅ Merged results saved to ${this.output}`);
  }

  createMergedDocument(files) {
    const parser = new DOMParser();
    const mergedDoc = parser.parseFromString(
      '<?xml version="1.0" encoding="UTF-8"?><testsuites></testsuites>'
    );
    
    const rootElement = mergedDoc.documentElement;
    let totalTests = 0;
    let totalFailures = 0;
    let totalErrors = 0;
    let totalSkipped = 0;
    let totalTime = 0;

    // Merge each file
    files.forEach((file, index) => {
      try {
        const content = fs.readFileSync(file, 'utf8');
        const doc = parser.parseFromString(content);
        
        // Get testsuites
        const testsuites = doc.getElementsByTagName('testsuite');
        
        for (let i = 0; i < testsuites.length; i++) {
          const testsuite = testsuites[i];
          
          // Update totals
          totalTests += parseInt(testsuite.getAttribute('tests') || '0');
          totalFailures += parseInt(testsuite.getAttribute('failures') || '0');
          totalErrors += parseInt(testsuite.getAttribute('errors') || '0');
          totalSkipped += parseInt(testsuite.getAttribute('skipped') || '0');
          totalTime += parseFloat(testsuite.getAttribute('time') || '0');
          
          // Add worker info to testsuite name
          const originalName = testsuite.getAttribute('name');
          testsuite.setAttribute('name', `${originalName} (Worker ${index + 1})`);
          
          // Import testsuite into merged document
          const importedSuite = mergedDoc.importNode(testsuite, true);
          rootElement.appendChild(importedSuite);
        }
      } catch (error) {
        console.error(`Error merging ${file}:`, error);
      }
    });

    // Update root element attributes
    rootElement.setAttribute('tests', totalTests.toString());
    rootElement.setAttribute('failures', totalFailures.toString());
    rootElement.setAttribute('errors', totalErrors.toString());
    rootElement.setAttribute('skipped', totalSkipped.toString());
    rootElement.setAttribute('time', totalTime.toFixed(3));
    rootElement.setAttribute('timestamp', new Date().toISOString());

    return mergedDoc;
  }

  createEmptyResults() {
    const emptyXml = `<?xml version="1.0" encoding="UTF-8"?>
<testsuites tests="0" failures="0" errors="0" skipped="0" time="0">
  <testsuite name="No tests executed" tests="0" failures="0" errors="0" skipped="0" time="0">
    <testcase name="No tests found" classname="NoTests" time="0">
      <system-out>No test results were generated</system-out>
    </testcase>
  </testsuite>
</testsuites>`;
    
    fs.writeFileSync(this.output, emptyXml);
  }
}

// Parse arguments
const args = process.argv.slice(2);
let workspace = '';
let output = '';

for (let i = 0; i < args.length; i += 2) {
  if (args[i] === '--workspace') {
    workspace = args[i + 1];
  } else if (args[i] === '--output') {
    output = args[i + 1];
  }
}

if (!workspace || !output) {
  console.error('Error: --workspace and --output are required');
  process.exit(1);
}

// Run merger
const merger = new TestResultMerger(workspace, output);
merger.merge();