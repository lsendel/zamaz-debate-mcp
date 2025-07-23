#!/usr/bin/env node

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');

console.log('🔍 Checking React imports and dependencies...\n');

// Check for multiple React versions
console.log('📦 Checking for multiple React versions...');
try {
  const reactVersions = execSync('npm ls react --depth=0', { encoding: 'utf-8', stdio: 'pipe' });
  const lines = reactVersions.split('\n');
  const directReact = lines.find(line => line.includes('├── react@') || line.includes('└── react@'));
  
  if (directReact) {
    console.log('✅ React version:', directReact.trim());
  }
} catch (error) {
  // npm ls might fail with invalid peer deps, but we can still continue
  console.warn('⚠️  Could not verify React version (this is normal with peer dep issues)');
}

// Check package.json for problematic dependencies
console.log('\n📋 Checking package.json for problematic UI libraries...');
const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf-8'));
const dependencies = { ...packageJson.dependencies, ...packageJson.devDependencies };

const problematicLibs = ['@zamaz/ui', '@custom/ui'];
const foundProblematic = Object.keys(dependencies).filter(dep => 
  problematicLibs.some(lib => dep.includes(lib))
);

if (foundProblematic.length > 0) {
  console.error('❌ Found problematic UI libraries:');
  foundProblematic.forEach(lib => console.error(`   - ${lib}`));
  console.error('\nThese libraries may bundle their own React!');
} else {
  console.log('✅ No problematic UI libraries found');
}

// Skip ESLint for now due to config conflicts
console.log('\n🔧 Skipping ESLint step (run "npm run lint" separately)...');

// Check for common import patterns
console.log('\n🔍 Checking for common import issues...');
const srcDir = path.join(process.cwd(), 'src');

function checkImports(dir) {
  const files = fs.readdirSync(dir);
  const issues = [];
  
  files.forEach(file => {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    
    if (stat.isDirectory()) {
      issues.push(...checkImports(filePath));
    } else if (file.match(/\.(ts|tsx|js|jsx)$/)) {
      const content = fs.readFileSync(filePath, 'utf-8');
      const relativePath = path.relative(process.cwd(), filePath);
      
      // Check for duplicate React imports
      const reactImports = content.match(/import.*from\s+['"]react['"]/g) || [];
      if (reactImports.length > 1) {
        issues.push(`${relativePath}: Multiple React imports detected`);
      }
      
      // Check for importing React internals
      if (content.match(/from\s+['"]react\/((?!index).)*['"]/)) {
        issues.push(`${relativePath}: Importing React internals`);
      }
      
      // Check for @zamaz/ui imports
      if (content.match(/from\s+['"]@zamaz\/ui['"]/)) {
        issues.push(`${relativePath}: Using @zamaz/ui - migrate to Ant Design`);
      }
      
      // Check for lucide-react imports
      if (content.match(/from\s+['"]lucide-react['"]/)) {
        issues.push(`${relativePath}: Using lucide-react - use @ant-design/icons instead`);
      }
    }
  });
  
  return issues;
}

const importIssues = checkImports(srcDir);
if (importIssues.length > 0) {
  console.error('❌ Found import issues:');
  importIssues.forEach(issue => console.error(`   - ${issue}`));
} else {
  console.log('✅ No common import issues found');
}

console.log('\n✨ React import validation complete!\n');