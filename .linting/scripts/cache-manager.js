#!/usr/bin/env node
/**
 * Linting Cache Manager
 *
 * This script manages the caching of linting results to improve performance
 * of incremental linting. It stores file hashes and linting results to avoid
 * re-linting files that haven't changed.
 */

const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// Configuration
const CACHE_DIR = path.resolve(process.cwd(), '.linting/cache');
const CACHE_FILE = path.join(CACHE_DIR, 'lint-cache.json');
const RESULTS_FILE = path.join(CACHE_DIR, 'lint-results.json');
const MAX_CACHE_AGE_DAYS = 7;

// Ensure cache directory exists
if (!fs.existsSync(CACHE_DIR)) {
  fs.mkdirSync(CACHE_DIR, { recursive: true });
}

// Load cache
let cache = {};
if (fs.existsSync(CACHE_FILE)) {
  try {
    cache = JSON.parse(fs.readFileSync(CACHE_FILE, 'utf8'));
  } catch (error) {
    console.error('Error loading cache:', error.message);
    cache = {};
  }
}

// Load results
let results = {
  timestamp: new Date().toISOString(),
  summary: {
    totalFiles: 0,
    filesWithIssues: 0,
    errors: 0,
    warnings: 0,
    info: 0,
    suggestions: 0,
  },
  issues: [],
};

if (fs.existsSync(RESULTS_FILE)) {
  try {
    results = JSON.parse(fs.readFileSync(RESULTS_FILE, 'utf8'));
  } catch (error) {
    console.error('Error loading results:', error.message);
  }
}

/**
 * Calculate hash for a file
 * @param {string} filePath - Path to the file
 * @returns {string} - Hash of the file content
 */
function calculateFileHash(filePath) {
  try {
    const content = fs.readFileSync(filePath);
    return crypto.createHash('md5').update(content).digest('hex');
  } catch (error) {
    console.error(`Error calculating hash for ${filePath}:`, error.message);
    return null;
  }
}

/**
 * Check if a file has changed since last lint
 * @param {string} filePath - Path to the file
 * @returns {boolean} - True if file has changed or wasn't cached
 */
function hasFileChanged(filePath) {
  if (!fs.existsSync(filePath)) {
    return false; // File doesn't exist
  }

  const currentHash = calculateFileHash(filePath);
  if (!currentHash) {
    return true; // Couldn't calculate hash, assume changed
  }

  const cachedInfo = cache[filePath];
  if (!cachedInfo || cachedInfo.hash !== currentHash) {
    return true; // Not in cache or hash changed
  }

  // Check if cache is too old
  const cacheDate = new Date(cachedInfo.timestamp);
  const now = new Date();
  const diffDays = (now - cacheDate) / (1000 * 60 * 60 * 24);

  return diffDays > MAX_CACHE_AGE_DAYS;
}

/**
 * Update cache for a file
 * @param {string} filePath - Path to the file
 * @param {object} lintResult - Linting result for the file
 */
function updateCache(filePath, lintResult = null) {
  if (!fs.existsSync(filePath)) {
    return; // File doesn't exist
  }

  const hash = calculateFileHash(filePath);
  if (!hash) {
    return; // Couldn't calculate hash
  }

  cache[filePath] = {
    hash,
    timestamp: new Date().toISOString(),
    lastLinted: new Date().toISOString(),
    hasIssues: lintResult ? lintResult.issues.length > 0 : false,
  };

  // Save cache
  fs.writeFileSync(CACHE_FILE, JSON.stringify(cache, null, 2));
}

/**
 * Add linting issues to results
 * @param {string} filePath - Path to the file
 * @param {Array} issues - Array of linting issues
 */
function addLintingIssues(filePath, issues) {
  if (!issues || !Array.isArray(issues) || issues.length === 0) {
    return;
  }

  // Update summary
  results.summary.totalFiles++;
  if (issues.length > 0) {
    results.summary.filesWithIssues++;
  }

  // Add issues to results
  issues.forEach(issue => {
    results.summary[issue.severity.toLowerCase()]++;
    results.issues.push({
      ...issue,
      file: filePath,
    });
  });

  // Save results
  fs.writeFileSync(RESULTS_FILE, JSON.stringify(results, null, 2));
}

/**
 * Reset linting results
 */
function resetResults() {
  results = {
    timestamp: new Date().toISOString(),
    summary: {
      totalFiles: 0,
      filesWithIssues: 0,
      errors: 0,
      warnings: 0,
      info: 0,
      suggestions: 0,
    },
    issues: [],
  };
  fs.writeFileSync(RESULTS_FILE, JSON.stringify(results, null, 2));
}

/**
 * Clean old cache entries
 */
function cleanCache() {
  const now = new Date();
  let cleaned = 0;

  Object.keys(cache).forEach(filePath => {
    // Remove entries for files that no longer exist
    if (!fs.existsSync(filePath)) {
      delete cache[filePath];
      cleaned++;
      return;
    }

    // Remove entries that are too old
    const cacheDate = new Date(cache[filePath].timestamp);
    const diffDays = (now - cacheDate) / (1000 * 60 * 60 * 24);
    if (diffDays > MAX_CACHE_AGE_DAYS) {
      delete cache[filePath];
      cleaned++;
    }
  });

  if (cleaned > 0) {
    console.log(`Cleaned ${cleaned} old cache entries`);
    fs.writeFileSync(CACHE_FILE, JSON.stringify(cache, null, 2));
  }
}

// Export functions
module.exports = {
  hasFileChanged,
  updateCache,
  addLintingIssues,
  resetResults,
  cleanCache,
};

// Handle command line arguments
if (require.main === module) {
  const args = process.argv.slice(2);
  const command = args[0];

  switch (command) {
    case 'clean':
      cleanCache();
      console.log('Cache cleaned');
      break;
    case 'reset':
      resetResults();
      console.log('Results reset');
      break;
    case 'check':
      const filePath = args[1];
      if (!filePath) {
        console.error('File path required');
        process.exit(1);
      }
      console.log(`File ${filePath} has changed: ${hasFileChanged(filePath)}`);
      break;
    case 'update':
      const updatePath = args[1];
      if (!updatePath) {
        console.error('File path required');
        process.exit(1);
      }
      updateCache(updatePath);
      console.log(`Cache updated for ${updatePath}`);
      break;
    default:
      console.log('Available commands: clean, reset, check <file>, update <file>');
  }
}
