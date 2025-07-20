# Zamaz UI Specifications - LLM Usage Guide

## Quick Start for LLMs

This design system provides all the specifications needed to create consistent, professional enterprise UIs. All files are structured for easy parsing and implementation.

### Directory Structure
```
ui-specs/
├── README.md                    # This file - main entry point
├── css/                        # Ready-to-use CSS files
│   ├── zamaz-design-system.css # Main CSS framework
│   ├── components.css          # Individual component styles
│   └── utilities.css           # Utility classes
├── images/                     # Design assets and icons
├── components/                 # Component specifications
│   ├── buttons.json           # Button component specs
│   ├── forms.json             # Form component specs
│   ├── cards.json             # Card component specs
│   └── ...                    # Other components
├── docs/                      # Documentation
│   └── CORPORATE_DESIGN_SYSTEM.md # Complete design guide
└── examples/                  # HTML examples
    ├── button-examples.html   # Button implementations
    ├── form-examples.html     # Form implementations
    └── layout-examples.html   # Layout patterns

```

## How to Use This Design System

### 1. For CSS Implementation
```html
<!-- Include the main CSS file -->
<link rel="stylesheet" href="ui-specs/css/zamaz-design-system.css">
```

### 2. For Component Creation
Check the JSON files in `/components/` for structured specifications:
- Each component has detailed properties
- Includes all variants and states
- Provides accessibility requirements

### 3. For Color Values
Access the color palette in `/ui-specs/design-tokens.json` or use CSS variables:
```css
var(--primary-500)  /* Main brand color: #0066ff */
var(--gray-900)     /* Text color: #212529 */
```

### 4. For Typography
Font stack and sizes are defined in CSS variables:
```css
font-family: var(--font-primary);  /* Inter font family */
font-size: var(--text-base);       /* 16px */
```

## Key Design Principles
1. **Mobile-first responsive design**
2. **WCAG 2.1 AA accessibility compliance**
3. **Performance-optimized**
4. **Dark mode support**

## Component Classes Quick Reference

### Buttons
- `.btn` - Base button class
- `.btn-primary` - Primary action button
- `.btn-secondary` - Secondary action button
- `.btn-ghost` - Minimal button style
- `.btn-sm`, `.btn-lg` - Size variants

### Forms
- `.form-input` - Text input fields
- `.form-select` - Dropdown selects
- `.form-textarea` - Text areas
- `.form-label` - Field labels
- `.form-help` - Helper text

### Layout
- `.container` - Responsive container
- `.grid` - CSS Grid container
- `.flex` - Flexbox container
- `.card` - Card component

## Design Tokens

All design decisions are tokenized in `/ui-specs/design-tokens.json` for easy consumption by build tools and applications.

## Examples

Ready-to-use HTML examples are provided in the `/examples/` directory. Each file demonstrates proper implementation of components following the design system.

---

For detailed specifications, refer to `/docs/CORPORATE_DESIGN_SYSTEM.md`