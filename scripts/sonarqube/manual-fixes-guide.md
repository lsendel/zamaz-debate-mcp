# Manual Fixes Guide for Remaining SonarCloud Issues

## Overview
This guide provides instructions for manually fixing the remaining complex SonarCloud issues that cannot be safely automated.

## High Priority Issues

### 1. Cognitive Complexity (S3776) - 34 occurrences
**Files affected**: Various JavaScript/TypeScript files

**How to fix**:
1. Break down large functions into smaller, focused functions
2. Extract complex conditional logic into separate methods
3. Replace nested if-else with early returns or switch statements
4. Use functional programming patterns (map, filter, reduce) instead of loops

**Example refactoring**:
```javascript
// Before
function complexFunction(data) {
    if (data) {
        if (data.type === 'A') {
            for (let i = 0; i < data.items.length; i++) {
                if (data.items[i].valid) {
                    // complex logic
                }
            }
        } else if (data.type === 'B') {
            // more complex logic
        }
    }
}

// After
function processData(data) {
    if (!data) return;
    
    const processor = getProcessor(data.type);
    return processor(data);
}

function getProcessor(type) {
    const processors = {
        'A': processTypeA,
        'B': processTypeB
    };
    return processors[type] || noOpProcessor;
}

function processTypeA(data) {
    return data.items
        .filter(item => item.valid)
        .map(processValidItem);
}
```

### 2. String Duplication in SQL (S1192) - 32 occurrences
**Files affected**: Migration files in `*/db/migration/`

**How to fix**:
1. For table names, column types, and constraints that repeat, define constants at the top
2. Use SQL variables or procedures for complex repeated logic

**Example**:
```sql
-- Add at top of migration file
-- Constants (as comments for documentation)
-- DEFAULT_VARCHAR_SIZE = 255
-- DEFAULT_TIMESTAMP_PRECISION = 6
-- AUDIT_COLUMNS = created_at, updated_at, created_by, updated_by

-- Use consistent patterns
CREATE TABLE organizations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    -- audit columns
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);
```

### 3. JavaScript Naming Conventions (S3504) - 226 occurrences
**Files affected**: Mostly test fixtures in `karate-api-tests/`

**How to fix**:
1. Review if PascalCase is intentional for test data
2. If not, convert to camelCase:
   - `GenerateDebateRequest` → `generateDebateRequest`
   - `CreateDebate` → `createDebate`

**Batch rename script**:
```bash
# In karate-api-tests directory
find . -name "*.js" -type f -exec sed -i '' \
    -e 's/GenerateDebateRequest/generateDebateRequest/g' \
    -e 's/CreateDebate/createDebate/g' \
    {} +
```

### 4. Function Parameter Order (S2234) - 110 occurrences
**Files affected**: Various JavaScript files

**How to fix**:
1. Check each function call to ensure parameters match the function signature
2. Common issue: swapping similar-typed parameters

**Example**:
```javascript
// Function definition
function updateUser(userId, userData, options) { }

// Wrong order
updateUser(userData, userId, options); // ❌

// Correct order
updateUser(userId, userData, options); // ✅
```

## Medium Priority Issues

### 5. Nested Ternary Operators (S3358)
**How to fix**:
Convert to if-else statements or extract to functions:

```javascript
// Before
const status = isActive ? (isPending ? 'pending' : 'active') : 'inactive';

// After
function getStatus(isActive, isPending) {
    if (!isActive) return 'inactive';
    return isPending ? 'pending' : 'active';
}
const status = getStatus(isActive, isPending);
```

### 6. Empty Catch Blocks (S2486)
**How to fix**:
Add appropriate error handling:

```javascript
try {
    // risky operation
} catch (error) {
    console.error('Operation failed:', error);
    // Consider: rethrow, return default, or notify monitoring
}
```

## Tools to Help

### 1. VSCode Extensions
- SonarLint: Real-time issue detection
- ESLint: JavaScript/TypeScript linting
- SQLTools: SQL formatting and linting

### 2. Automated Refactoring Tools
- `js-codemod`: Facebook's JavaScript codemod scripts
- `jscodeshift`: JavaScript refactoring toolkit

### 3. Scripts for Batch Changes
```bash
# Find all files with specific issues
grep -r "function.*[A-Z]" --include="*.js" .

# Count occurrences of specific patterns
find . -name "*.js" -exec grep -c "var " {} + | grep -v ":0$"
```

## Testing After Fixes

1. Run unit tests: `npm test`
2. Run integration tests: `mvn test`
3. Run linting: `npm run lint`
4. Check with SonarLint locally before pushing

## Prioritization

1. **Fix first**: Issues that affect code behavior or security
2. **Fix second**: Issues that affect maintainability
3. **Consider ignoring**: Issues in generated code or intentional patterns

## Verification

After making fixes:
1. Run local SonarQube scanner: `make sonarqube-scan`
2. Check the quality gate status
3. Ensure no regressions in functionality