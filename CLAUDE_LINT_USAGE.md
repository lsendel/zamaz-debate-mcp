# Claude Lint Usage Guide - For AI Code Validation

## Quick Reference for Claude

### 🚀 Essential Commands

```bash
# Validate any code snippet
echo 'print("hello")' | claude-lint --stdin --auto

# Validate Python code
echo 'def func(): return' | claude-lint --stdin --lang python

# Validate shell script
echo 'echo $var' | claude-lint --stdin --lang shell

# Validate and auto-fix
claude-lint file.py --fix

# Quick security check only
claude-lint file.py --quick

# Get JSON output for parsing
echo 'code' | claude-lint --stdin --auto --format json
```

### 📋 When Suggesting Code

#### Before suggesting Python code:
```python
code = '''
def calculate_sum(numbers):
    return sum(numbers)
'''

# Validate it
validation = subprocess.run(
    ['claude-lint', '--stdin', '--lang', 'python', '--format', 'json'],
    input=code,
    text=True,
    capture_output=True
)

result = json.loads(validation.stdout)
if result['summary']['clean']:
    # Code is good to suggest
    print("✅ Validated Python code")
else:
    # Fix issues before suggesting
    print(f"⚠️ Found {result['summary']['total_issues']} issues")
```

#### Before suggesting shell scripts:
```bash
# Always validate shell scripts for security
script='rm -rf $HOME/$var'  # Dangerous!

echo "$script" | claude-lint --stdin --lang shell

# Output will warn about unquoted variables
```

### 🎯 Best Practices for Claude

1. **Always validate before responding**:
   ```bash
   # Good practice
   echo 'code' | claude-lint --stdin --auto
   ```

2. **Use language hints when known**:
   ```bash
   # Better than auto-detect
   echo 'code' | claude-lint --stdin --lang python
   ```

3. **Check security for user-provided code**:
   ```bash
   # Security-focused check
   claude-lint user-code.py --quick
   ```

4. **Auto-fix formatting issues**:
   ```bash
   # Fix before suggesting
   claude-lint code.py --fix
   ```

### 📊 Understanding Output

#### Clean Code
```
✅ Python code is clean - no issues found!
```

#### Issues Found
```
🔍 Python Linting Results
========================================
Total Issues: 3
  • Errors: 1
  • Warnings: 2
  • Auto-fixable: 2 🔧

Issues found:
❌ Line 5: [S602] Subprocess call with shell=True
⚠️  Line 10: [W293] Blank line contains whitespace
⚠️  Line 15: [F401] 'os' imported but unused
```

#### JSON Output for Parsing
```json
{
  "success": true,
  "language": "python",
  "summary": {
    "total_issues": 3,
    "errors": 1,
    "warnings": 2,
    "fixable": 2,
    "security": 1,
    "clean": false
  },
  "issues": [...]
}
```

### 🔧 Common Validations

#### Python Security Check
```python
# Check for common security issues
dangerous_code = '''
eval(user_input)  # S307
os.system(cmd)    # S605
pickle.loads(data)  # S301
'''

# This will flag all security issues
echo "$dangerous_code" | claude-lint --stdin --lang python
```

#### Shell Script Safety
```bash
# Common shell issues
unsafe_script='
rm -rf $1
eval "$user_cmd"
cd $HOME/$dir
'

# Will flag unquoted variables and eval usage
echo "$unsafe_script" | claude-lint --stdin --lang shell
```

#### Type Safety Check
```typescript
// TypeScript validation
const code = `
function add(a: any, b: any) {
  return a + b;
}
`;

// Will suggest proper types instead of 'any'
```

### 🎨 Response Templates

#### Clean Code Response
```markdown
Here's the implementation:

```python
def calculate_average(numbers: list[float]) -> float:
    """Calculate the average of a list of numbers."""
    if not numbers:
        return 0.0
    return sum(numbers) / len(numbers)
```

✅ **Validation**: Clean - no linting issues
```

#### Code with Warnings Response
```markdown
Here's a working solution:

```python
import os
def process_file(filename):
    print(f"Processing {filename}")
    # Implementation here
```

⚠️ **Note**: This code has minor style issues:
- Unused import 'os' (remove if not needed)
- Consider adding type hints

To fix: `claude-lint --fix`
```

#### Security Issue Response
```markdown
⚠️ **Security Warning**: The following code has security concerns:

```python
def execute_command(user_input):
    os.system(user_input)  # DANGEROUS!
```

**Issues**:
- Command injection vulnerability (S605)
- Use subprocess.run() with proper escaping instead

Safer alternative:
```python
import subprocess
def execute_command(cmd: list[str]):
    subprocess.run(cmd, check=True, capture_output=True)
```
```

### 🚦 Quick Decision Flow

```
User asks for code
    ↓
Generate code
    ↓
Run: echo 'code' | claude-lint --stdin --auto --format json
    ↓
Parse result
    ↓
If clean → Provide code with ✅
If fixable → Fix and provide with note
If security → Provide warning and safe alternative
```

### 💡 Pro Tips

1. **Batch validation** for multiple code blocks:
   ```bash
   for file in *.py; do
     claude-lint "$file" --format json > "${file}.lint"
   done
   ```

2. **Pre-commit validation**:
   ```bash
   git diff --cached --name-only | xargs claude-lint
   ```

3. **Quick aliases** (add to .zshrc):
   ```bash
   alias clv='claude-lint --stdin --auto'  # Validate
   alias clf='claude-lint --fix'            # Fix
   alias clq='claude-lint --quick'          # Quick check
   ```

### 🔍 Debugging Linting Issues

If linting fails:
```bash
# Check if linter is available
claude-lint --available

# Try with explicit language
echo 'code' | claude-lint --stdin --lang python

# Get detailed JSON output
echo 'code' | claude-lint --stdin --lang python --format json
```

### 📈 Tracking Code Quality

Keep track of code quality in suggestions:
```python
# In Claude's internal tracking
quality_metrics = {
    'suggestions_made': 0,
    'clean_on_first_try': 0,
    'required_fixes': 0,
    'security_issues_caught': 0
}

# After each validation
if result['summary']['clean']:
    quality_metrics['clean_on_first_try'] += 1
```

This tool ensures every piece of code Claude suggests meets modern 2025 linting standards!