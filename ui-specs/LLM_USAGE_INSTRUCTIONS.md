# LLM Usage Instructions - Zamaz Design System

## Quick Reference for AI Implementation

### 1. When Asked to Create a UI Component

**Step 1: Check design-tokens.json**
```
Path: /ui-specs/design-tokens.json
Purpose: Get exact color values, spacing, typography
```

**Step 2: Reference component specifications**
```
Path: /ui-specs/components/{component-name}.json
Purpose: Get props, variants, states, and examples
```

**Step 3: Use the CSS framework**
```
Path: /ui-specs/css/zamaz-design-system.css
Purpose: Apply pre-built classes
```

### 2. React Component Creation Template

```typescript
// Always follow this structure for React components:

import React from 'react';
import { clsx } from 'clsx';

interface ComponentProps {
  variant?: 'primary' | 'secondary' | 'ghost';
  size?: 'sm' | 'md' | 'lg';
  className?: string;
  children: React.ReactNode;
}

export const Component: React.FC<ComponentProps> = ({
  variant = 'primary',
  size = 'md',
  className,
  children,
  ...props
}) => {
  return (
    <div 
      className={clsx(
        'base-classes',
        `variant-${variant}`,
        `size-${size}`,
        className
      )}
      {...props}
    >
      {children}
    </div>
  );
};
```

### 3. Color Usage Cheat Sheet

```css
/* Primary Actions */
background: var(--primary-500);  /* #0066ff */

/* Secondary Actions */
background: var(--secondary-500); /* #1a9b69 */

/* Text Colors */
color: var(--text-primary);      /* #212529 */
color: var(--text-secondary);    /* #495057 */

/* Backgrounds */
background: var(--bg-primary);    /* #ffffff */
background: var(--bg-secondary);  /* #f8f9fa */

/* Status Colors */
color: var(--success);           /* #10b981 */
color: var(--error);             /* #ef4444 */
color: var(--warning);           /* #f59e0b */
```

### 4. Common Component Patterns

#### Button Implementation
```jsx
// From /ui-specs/components/buttons.json
<Button 
  variant="primary"    // primary | secondary | ghost
  size="md"           // sm | md | lg
  loading={false}     // Shows spinner
  disabled={false}    // Disables interaction
  onClick={handler}
>
  Click Me
</Button>
```

#### Form Field Implementation
```jsx
// From /ui-specs/components/forms.json
<div className="form-group">
  <label className="form-label">
    Email Address
    <span className="text-error">*</span>
  </label>
  <input 
    type="email"
    className="form-input"
    placeholder="user@example.com"
    required
  />
  <p className="form-help">We'll never share your email</p>
</div>
```

#### Card Layout
```jsx
// From /ui-specs/components/cards.json
<div className="card">
  <div className="card-header">
    <h3 className="card-title">Title</h3>
  </div>
  <div className="card-body">
    Content goes here
  </div>
  <div className="card-footer">
    Footer actions
  </div>
</div>
```

### 5. Responsive Design Rules

```css
/* Mobile First Approach */
/* Base (Mobile) */
.element { 
  padding: var(--space-3); /* 12px */
}

/* Tablet (768px+) */
@media (min-width: 768px) {
  .md\:padding-4 { 
    padding: var(--space-4); /* 16px */
  }
}

/* Desktop (1024px+) */
@media (min-width: 1024px) {
  .lg\:padding-6 { 
    padding: var(--space-6); /* 24px */
  }
}
```

### 6. Accessibility Checklist

- [ ] All interactive elements have focus states
- [ ] Color contrast meets WCAG AA (4.5:1)
- [ ] Touch targets are minimum 44x44px
- [ ] Forms have proper labels and ARIA attributes
- [ ] Keyboard navigation works correctly
- [ ] Screen reader announcements are included

### 7. File Reference Map

```
/ui-specs/
├── README.md                     → Start here for overview
├── design-tokens.json            → All design values
├── css/
│   └── zamaz-design-system.css   → Complete CSS framework
├── components/
│   ├── buttons.json              → Button specifications
│   ├── forms.json                → Form specifications
│   └── cards.json                → Card specifications
├── docs/
│   └── CORPORATE_DESIGN_SYSTEM.md → Detailed guidelines
├── examples/
│   ├── button-examples.html      → Button demos
│   └── layout-examples.html      → Layout patterns
└── REACT_IMPLEMENTATION_GUIDE.md → React best practices
```

### 8. Common Implementation Tasks

#### Task: Create a login form
1. Check `forms.json` for input specifications
2. Use grid system from `layout-examples.html`
3. Apply button styles from `buttons.json`
4. Follow accessibility guidelines

#### Task: Build a dashboard
1. Reference `layout-examples.html` for dashboard layout
2. Use card components from `cards.json`
3. Apply spacing from `design-tokens.json`
4. Implement responsive grid

#### Task: Style a navigation bar
1. Check navbar example in `layout-examples.html`
2. Apply typography from design tokens
3. Use appropriate spacing values
4. Ensure mobile responsiveness

### 9. CSS Class Naming Convention

```css
/* Component Classes */
.btn           /* Base component */
.btn-primary   /* Variant modifier */
.btn-lg        /* Size modifier */

/* Utility Classes */
.text-primary  /* Text utilities */
.bg-secondary  /* Background utilities */
.p-4          /* Padding utilities */
.m-auto       /* Margin utilities */

/* State Classes */
.is-active     /* Active state */
.is-disabled   /* Disabled state */
.is-loading    /* Loading state */
```

### 10. Quick Validation

Before delivering any UI implementation:
1. ✓ Uses correct color tokens
2. ✓ Follows spacing system (4px base)
3. ✓ Includes hover/focus states
4. ✓ Works on mobile (responsive)
5. ✓ Meets accessibility standards
6. ✓ Uses semantic HTML
7. ✓ Follows component patterns

---

**Remember**: This design system represents Fortune 100 enterprise standards. Always prioritize:
- Professional appearance
- Accessibility
- Performance
- Consistency
- User experience