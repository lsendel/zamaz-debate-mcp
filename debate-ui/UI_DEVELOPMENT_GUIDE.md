# UI Development Guide - Best Practices & Lessons Learned

## Quick Reference for LLM/AI Assistants

### 🚨 Critical Rules
1. **NEVER use mock components** - Always use real UI libraries
2. **NEVER hardcode ports** - Use environment variables
3. **ALWAYS validate with Puppeteer** before saying "it works"
4. **ALWAYS use consistent UI library** throughout the app

## When Creating New UI Components

### 1. Choose the Right UI Library
```typescript
// ✅ GOOD - Established, well-maintained libraries
import { Button, Card, Modal } from 'antd';           // Ant Design
import { Button, Card, Dialog } from '@mui/material'; // Material-UI
import { Button, Card, Modal } from '@chakra-ui/react'; // Chakra UI

// ❌ BAD - Custom or problematic libraries
import { Button } from '@custom/ui';  // May bundle React
import { Card } from 'some-random-ui'; // Unknown maintenance
```

### 2. Check for React Conflicts BEFORE Installing
```bash
# Check if library bundles React (BAD)
npm view @some-library/ui dependencies

# Library should have React as peerDependency (GOOD)
npm view antd peerDependencies

# After installation, verify single React instance
npm ls react
```

### 3. Component Import Patterns
```typescript
// ✅ GOOD - Consistent imports
import { Button, Form, Input, Modal, Select, Table } from 'antd';
import { UserOutlined, SettingOutlined } from '@ant-design/icons';

// ❌ BAD - Mixed libraries
import { Button } from 'antd';
import { Modal } from '@mui/material';
import { User } from 'lucide-react';
```

### 4. Styling Patterns
```typescript
// ✅ GOOD - When NOT using Tailwind
<div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
<Card style={{ marginBottom: '24px' }}>

// ✅ GOOD - When using Ant Design
<Space direction="vertical" size="large">
<Row gutter={16}>

// ❌ BAD - Tailwind classes without Tailwind setup
<div className="flex items-center gap-4">
<div className="mb-6 p-4 rounded-lg">
```

### 5. Form Patterns
```typescript
// ✅ GOOD - Ant Design Forms
<Form layout="vertical" onFinish={handleSubmit}>
  <Form.Item label="Username" name="username" rules={[{ required: true }]}>
    <Input />
  </Form.Item>
</Form>

// ❌ BAD - Custom form components
<FormField label="Username">
  <Input />
</FormField>
```

### 6. Modal/Dialog Patterns
```typescript
// ✅ GOOD - Ant Design Modal
<Modal
  open={isOpen}
  onCancel={handleClose}
  title="Create Item"
  footer={[
    <Button key="cancel" onClick={handleClose}>Cancel</Button>,
    <Button key="submit" type="primary" onClick={handleSubmit}>Submit</Button>
  ]}
>
  {/* content */}
</Modal>

// ❌ BAD - Custom Dialog components
<Dialog open={isOpen}>
  <DialogContent>
    {/* content */}
  </DialogContent>
</Dialog>
```

### 7. Notification Patterns
```typescript
// ✅ GOOD - Ant Design notifications
import { notification } from 'antd';

notification.success({
  message: 'Success',
  description: 'Operation completed successfully',
});

// ❌ BAD - Custom toast components
<Toast message="Success" />
```

## Environment Variables

### Always Use .env for Configuration
```typescript
// ✅ GOOD
const API_URL = process.env.VITE_API_URL || 'http://localhost:5000';
const UI_PORT = process.env.VITE_PORT || 3001;

// ❌ BAD
const API_URL = 'http://localhost:5000';
const UI_PORT = 3001;
```

## Testing Checklist

### Before Saying "It Works"
1. Run Puppeteer test to verify no blank screens
2. Check browser console for errors
3. Take screenshots of each major section
4. Verify all dropdowns have data
5. Test form submissions
6. Check responsive behavior

### Validation Script Template
```javascript
const puppeteer = require('puppeteer');

async function validateUI() {
  const browser = await puppeteer.launch({ headless: false });
  const page = await browser.newPage();
  
  // Monitor console errors
  page.on('console', msg => {
    if (msg.type() === 'error') {
      console.error('Browser error:', msg.text());
    }
  });
  
  // Navigate and check
  await page.goto('http://localhost:3001');
  const hasContent = await page.$('.ant-layout') !== null;
  
  if (!hasContent) {
    throw new Error('UI is not rendering properly!');
  }
  
  await browser.close();
}
```

## Common Migration Patterns

### From @zamaz/ui to Ant Design
| @zamaz/ui | Ant Design | Notes |
|-----------|-----------|-------|
| `<Dialog>` | `<Modal>` | Different prop names |
| `<DialogContent>` | Modal children | No wrapper needed |
| `<Toast>` | `notification.success()` | API vs Component |
| `<Badge variant="success">` | `<Badge status="success">` | Different prop name |
| `<DataTable>` | `<Table>` | Different column format |
| `<CircularProgress>` | `<Spin>` | Built-in spinner |
| `<Select><SelectTrigger>` | `<Select>` | Simpler API |

## Debugging Tips

### React Version Conflicts
```bash
# Find duplicate React
npm ls react

# Clear cache and reinstall
rm -rf node_modules package-lock.json
npm install

# Verify single React
npm ls react
```

### Console Errors to Watch For
- "ReactCurrentDispatcher" - Multiple React versions
- "Invalid hook call" - React version mismatch
- "Cannot read properties of undefined" - Missing provider/context

## Quick Fixes

### Blank Screen?
1. Check browser console for errors
2. Verify all imports are from same UI library
3. Run `npm ls react` to check for duplicates
4. Look for remaining old UI library imports

### Empty Dropdowns?
1. Check if backend services are running
2. Verify API endpoints in .env
3. Check network tab for failed requests
4. Never mock data - fix the backend connection

### Style Issues?
1. Remove all Tailwind classes if not using Tailwind
2. Use inline styles or UI library's styling system
3. Check for conflicting CSS imports

## Remember
- User wants REAL data, not mocks
- Test with Puppeteer before claiming success
- Keep UI library consistent throughout app
- Document any new patterns in CLAUDE.md