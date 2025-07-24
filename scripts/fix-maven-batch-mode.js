#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

const workflowsDir = path.join(__dirname, '..', '.github', 'workflows');
const mavenBatchMode = "MAVEN_BATCH_MODE: '--batch-mode --no-transfer-progress --show-version'";

// Workflows that already have MAVEN_BATCH_MODE
const workflowsWithBatchMode = [
  'ci-cd-with-failure-handler.yml',
  'ci.yml',
  'intelligent-ci.yml'
];

// Find all workflow files
const workflowFiles = fs.readdirSync(workflowsDir)
  .filter(file => file.endsWith('.yml') || file.endsWith('.yaml'));

console.log('🔧 Fixing Maven batch mode in workflows...\n');

workflowFiles.forEach(file => {
  const filePath = path.join(workflowsDir, file);
  let content = fs.readFileSync(filePath, 'utf8');
  let modified = false;
  
  // Check if file has Maven commands
  if (!content.includes('mvn ')) {
    return;
  }
  
  console.log(`📄 Processing ${file}...`);
  
  // Add MAVEN_BATCH_MODE to env section if not present
  if (!workflowsWithBatchMode.includes(file) && !content.includes('MAVEN_BATCH_MODE:')) {
    // Find env: section
    const envMatch = content.match(/^env:\s*$/m);
    if (envMatch) {
      // Add to existing env section
      const envIndex = content.indexOf(envMatch[0]) + envMatch[0].length;
      const nextLineIndex = content.indexOf('\n', envIndex);
      const indent = content.substring(envIndex, nextLineIndex).match(/^\s*/)?.[0] || '  ';
      content = content.substring(0, nextLineIndex) + '\n' + indent + mavenBatchMode + content.substring(nextLineIndex);
      modified = true;
      console.log('  ✅ Added MAVEN_BATCH_MODE to env section');
    } else {
      // Add env section after on: section
      const onMatch = content.match(/^on:\s*\n([\s\S]*?)^\w/m);
      if (onMatch) {
        const insertIndex = content.indexOf(onMatch[0]) + onMatch[0].length - 1;
        content = content.substring(0, insertIndex) + '\nenv:\n  ' + mavenBatchMode + '\n\n' + content.substring(insertIndex);
        modified = true;
        console.log('  ✅ Created env section with MAVEN_BATCH_MODE');
      }
    }
  }
  
  // Replace Maven commands
  const mavenCommands = [
    // Simple replacements
    { from: /mvn clean test\b/g, to: 'mvn clean test ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn clean compile -B\b/g, to: 'mvn clean compile ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn clean package -DskipTests\b/g, to: 'mvn clean package -DskipTests ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn clean package -DskipTests -B\b/g, to: 'mvn clean package -DskipTests ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn test -B\b/g, to: 'mvn test ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn verify -P integration-test -B\b/g, to: 'mvn verify -P integration-test ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn test -Dtest=\*PactTest -B\b/g, to: 'mvn test -Dtest=*PactTest ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn pact:publish -B\b/g, to: 'mvn pact:publish ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn test -Dtest=SecurityTestSuite -B\b/g, to: 'mvn test -Dtest=SecurityTestSuite ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn org\.owasp:dependency-check-maven:check -B\b/g, to: 'mvn org.owasp:dependency-check-maven:check ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn gatling:test -B\b/g, to: 'mvn gatling:test ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn verify -Pintegration-tests\b/g, to: 'mvn verify -Pintegration-tests ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn clean compile -DskipTests\b/g, to: 'mvn clean compile -DskipTests ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn versions:update-properties\b/g, to: 'mvn versions:update-properties ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn versions:use-latest-releases\b/g, to: 'mvn versions:use-latest-releases ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn checkstyle:check -q\b/g, to: 'mvn checkstyle:check -q ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn spotbugs:check -q\b/g, to: 'mvn spotbugs:check -q ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn pmd:check -q\b/g, to: 'mvn pmd:check -q ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn checkstyle:checkstyle spotbugs:spotbugs pmd:pmd -q\b/g, to: 'mvn checkstyle:checkstyle spotbugs:spotbugs pmd:pmd -q ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn clean test -pl github-integration\b/g, to: 'mvn clean test -pl github-integration ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn verify -pl github-integration\b/g, to: 'mvn verify -pl github-integration ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn org\.owasp:dependency-check-maven:check -pl github-integration\b/g, to: 'mvn org.owasp:dependency-check-maven:check -pl github-integration ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn spotbugs:check -Dspotbugs\.skip=true\b/g, to: 'mvn spotbugs:check -Dspotbugs.skip=true ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn checkstyle:check -Dcheckstyle\.skip=true\b/g, to: 'mvn checkstyle:check -Dcheckstyle.skip=true ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn pmd:check -Dpmd\.skip=true\b/g, to: 'mvn pmd:check -Dpmd.skip=true ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn clean verify sonar:sonar\b/g, to: 'mvn clean verify sonar:sonar ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn clean compile\b/g, to: 'mvn clean compile ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn test\b(?! \$\{\{ env\.MAVEN_BATCH_MODE \}\})/g, to: 'mvn test ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn surefire-report:report\b/g, to: 'mvn surefire-report:report ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn verify -Pintegration-tests\b/g, to: 'mvn verify -Pintegration-tests ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn checkstyle:check -q -Dcheckstyle\.includeTestSourceDirectory=false\b/g, to: 'mvn checkstyle:check -q -Dcheckstyle.includeTestSourceDirectory=false ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn org\.apache\.maven\.plugins:maven-dependency-plugin:3\.6\.1:tree/g, to: 'mvn org.apache.maven.plugins:maven-dependency-plugin:3.6.1:tree ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn clean compile -B -V -T 1C/g, to: 'mvn clean compile -T 1C ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn test -B -V\b/g, to: 'mvn test ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn verify -B -V\b/g, to: 'mvn verify ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn org\.owasp:dependency-check-maven:check\b(?! \$\{\{ env\.MAVEN_BATCH_MODE \}\})/g, to: 'mvn org.owasp:dependency-check-maven:check ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn package -B -V\b/g, to: 'mvn package ${{ env.MAVEN_BATCH_MODE }}' },
    { from: /mvn dependency:tree -Dverbose\b/g, to: 'mvn dependency:tree -Dverbose ${{ env.MAVEN_BATCH_MODE }}' }
  ];
  
  let commandsFixed = 0;
  mavenCommands.forEach(cmd => {
    const matches = content.match(cmd.from);
    if (matches) {
      content = content.replace(cmd.from, cmd.to);
      commandsFixed += matches.length;
      modified = true;
    }
  });
  
  if (commandsFixed > 0) {
    console.log(`  ✅ Fixed ${commandsFixed} Maven commands`);
  }
  
  if (modified) {
    fs.writeFileSync(filePath, content);
    console.log(`  💾 Saved changes to ${file}\n`);
  } else {
    console.log(`  ℹ️  No changes needed\n`);
  }
});

console.log('✅ Maven batch mode fixes complete!');